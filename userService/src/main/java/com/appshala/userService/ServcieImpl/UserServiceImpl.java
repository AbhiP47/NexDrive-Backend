package com.appshala.userService.ServcieImpl;

import com.appshala.userService.Client.GroupServiceClient;
import com.appshala.userService.Enum.Role;
import com.appshala.userService.Enum.SortDirection;
import com.appshala.userService.Enum.Status;
import com.appshala.userService.Enum.UserSortBy;
import com.appshala.userService.event.UserDeletedEvent;
import com.appshala.userService.event.UserInvitedEvent;
import com.appshala.userService.EventProducer.UserDeletedEventProducer;
import com.appshala.userService.EventProducer.UserInvitedEventProducer;
import com.appshala.userService.Model.User;
import com.appshala.userService.Payloads.UserCreationRequest;
import com.appshala.userService.Payloads.UserRequest;
import com.appshala.userService.Payloads.UserResponse;
import com.appshala.userService.Payloads.*;
import com.appshala.userService.Repository.UserRepository;
import com.appshala.userService.Service.UserService;
import com.appshala.userService.Util.Helper;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final GroupServiceClient groupServiceClient;

    private final UserInvitedEventProducer userInvitedEventProducer;
    private final UserDeletedEventProducer userDeletedEventProducer;

    private final KafkaTemplate<String , UserDeletedEvent> kafkaTemplate;

    private final String userTopic;


    private final Helper helper;

    public UserServiceImpl(UserRepository userRepository , GroupServiceClient groupServiceClient, UserDeletedEventProducer userDeletedEventProducer , UserInvitedEventProducer userInvitedEventProducer , KafkaTemplate<String , UserDeletedEvent> kafkaTemplate,
                           @Value("${kafka-topic.user-events}") String userTopic  , Helper helper)
    {
        this.userRepository = userRepository;
        this.groupServiceClient = groupServiceClient;
        this.kafkaTemplate = kafkaTemplate;
        this.userTopic = userTopic;
        this.userInvitedEventProducer = userInvitedEventProducer;
        this.userDeletedEventProducer = userDeletedEventProducer;
        this.helper = helper;
    }


    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest userCreationRequest, UUID adminId) {
        Role adminRole = userRepository.findRoleById(adminId)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found for ID: " + adminId));
        if (adminRole != Role.ADMIN && adminRole != Role.SUPERADMIN) {
            throw new IllegalStateException("Unauthorized: Only authorized admins can create users.");
        }
        Role targetRole = userCreationRequest.getRole();
        if(!helper.isAuthorizedToCreate(adminRole,targetRole))
        {
            log.warn(("Admin {} (Role: {}) attempted to create unauthorized role: {}"), adminId, adminRole, targetRole);
            throw new SecurityException("Admin role " + adminRole + " is not authorized to create users with role " + targetRole);
        }
        String token = helper.generateSecureToken();
        LocalDateTime expirationTime = LocalDateTime.now().plusDays(7);
        User user = User.builder()
                .name(userCreationRequest.getName())
                .email(userCreationRequest.getEmail())
                .role(userCreationRequest.getRole())
                .status(Status.INVITED)
                .createdBy(adminId)
                .updatedBy(adminId)
                .invitationToken(token)
                .tokenExpiresAt(expirationTime)
                .build();
        User savedUser = userRepository.save(user);

        UserInvitedEvent userInvitedEvent = new UserInvitedEvent(savedUser.getId().toString() , savedUser.getName() , savedUser.getEmail(), savedUser.getInvitationToken() , Instant.now().toString());
        userInvitedEventProducer.publishUserInvitedEvent(userInvitedEvent);
        return helper.convertToUserResponse(savedUser);
    }





    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserResponse> findAll(UUID adminId) {
        List<User> users = userRepository.findAllByCreatedBy(adminId);
        return users.stream()
                .map(helper::convertToUserResponse)
                .collect(Collectors.toList());

    }

    @Override
    public boolean existByEmail(String email) {
        return false;
    }


    public Page<UserResponse> findUsers(
            Role role,
            Status status,
            String userGroupName,
            UserSortBy sortBy,
            SortDirection sortDirection,
            int page,
            int size,
            UUID adminId
    ) {

        Sort.Direction direction = sortDirection.getDirection();

        Sort sort = Sort.by(direction, sortBy.getDbField());
        Pageable pageable = PageRequest.of(page, size, sort);

        List<UUID> memberUserIds = getMemberIdsByGroupName(userGroupName , adminId);
        if(memberUserIds == null || memberUserIds.isEmpty())
            return Page.empty(pageable);

        //  Execute Query with only Role and Status filters
        return userRepository.findAll(
                buildSpecification(role, status, memberUserIds ),
                pageable
        ).map(helper::convertToUserResponse);
    }


    private Specification<User> buildSpecification(
            Role role,
            Status status,
            List<UUID> userIds) {
        return (root, query, criteriaBuilder) -> {
            // List to hold active filter conditions
            List<Predicate> predicates = new ArrayList<>();

            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (userIds != null && !userIds.isEmpty()) {
                predicates.add(root.get("id").in(userIds));
            }
            // Combine all active predicates with an AND logical operator
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getMemberIdsByGroupName(String groupName, UUID adminId) {

        if (groupName == null || groupName.isEmpty() || adminId == null) {
            return Collections.emptyList();
        }

        UUID groupId;
        List<UUID> memberUserIds = Collections.emptyList();

        try {
            groupId = groupServiceClient.getGroupIdByName(groupName, adminId);

            memberUserIds = groupServiceClient.getMemberUserIdsByGroupId(groupId);

        } catch (RuntimeException e) {

            throw new RuntimeException("Could not retrieve members for group '" + groupName + "'. Reason: " + e.getMessage(), e);
        }

        if (memberUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        return memberUserIds;
    }

    @Override
    public UUID getCurrentAdminId(UUID adminID) {
        return null;
    }



    @Override
    public List<UserResponse> createUsers(List<UserCreationRequest> userCreationRequests, UUID adminId) {

        Role adminRole = userRepository.findRoleById(adminId).orElseThrow(
                ()-> new UsernameNotFoundException("Admin not found for ID : "+adminId)
        );
        if (adminRole != Role.ADMIN && adminRole != Role.SUPERADMIN) {
            throw new IllegalStateException("Unauthorized: Only authorized admins can create users.");
        }

        List<UserResponse> userResponses = new ArrayList<>();
        for(UserCreationRequest userCreationRequest : userCreationRequests)
        {
            Role targetRole = userCreationRequest.getRole();
            if(!helper.isAuthorizedToCreate(adminRole,targetRole))
            {
                log.error("Admin {} (Role: {}) attempted to create unauthorized role: {}", adminId, adminRole, targetRole);
                throw new SecurityException("Admin role " + adminRole + " is not authorized to create users with role " + targetRole);
            }
            String token = helper.generateSecureToken();
            String  timeStamp = LocalDateTime.now().plusDays(7).toString();
            User user =
            User.builder()
                    .name(userCreationRequest.getName())
                    .email(userCreationRequest.getEmail())
                    .role(userCreationRequest.getRole())
                    .createdBy(adminId)
                    .updatedBy(adminId)
                   .build();
            User savedUser = userRepository.save(user);
            UserInvitedEvent event = new UserInvitedEvent(savedUser.getId().toString(),userCreationRequest.getName(),userCreationRequest.getEmail().toLowerCase().trim(),token,timeStamp);
            try {
                userInvitedEventProducer.publishUserInvitedEvent(event);
                userResponses.add(helper.convertToUserResponse(savedUser));
            }
            catch (Exception e)
            {
                log.warn("KAKFA : failed to send invite to the user : {} , with email : {} , deleting the user from the database .",userCreationRequest.getName(),userCreationRequest.getEmail());
                userRepository.delete(savedUser);
            }

        }
        return userResponses;
    }

    @Override
    public void deleteUserById(UUID id) {
        User user  = userRepository.findById(id)
                .orElseThrow(()-> new UsernameNotFoundException("User not found with ID :"+id));
        userRepository.delete(user);
        log.info("user deleted successfully from the database : ID {}",id);
        UserDeletedEvent event = new UserDeletedEvent(id, Instant.now().toString());
        userDeletedEventProducer.publishUserDeletedEvent(event);

    }

    @Override
    public UserResponse updateUserById(UUID id , UserRequest userRequest) {
        User user = userRepository.findById(id).orElseThrow(()-> new UsernameNotFoundException("user not found with ID: "+id));
        if(userRequest.getName() != null)
            user.setName(userRequest.getName());

        if(userRequest.getStatus() != null)
            user.setStatus(userRequest.getStatus());
        User savedUser = userRepository.save(user);
        return helper.convertToUserResponse(savedUser);
    }
    @Override
    public boolean checkUserExistsById(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public ImportResult processBulkImport(MultipartFile file, UUID adminId) throws Exception {

        Role adminRole = userRepository.findRoleById(adminId)
                .orElseThrow(()-> new UsernameNotFoundException("ADMIN not found with ID : "+adminId));
        if (adminRole != Role.ADMIN && adminRole != Role.SUPERADMIN) {
            throw new RuntimeException("Unauthorized: Only ADMIN or SUPERADMIN can perform bulk import.");
        }


        List<UserCsvRecord> allRecords;

        try(Reader reader = new InputStreamReader(file.getInputStream()))
        {
            allRecords = new CsvToBeanBuilder<UserCsvRecord>(reader)
                    .withType(UserCsvRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
        catch(Exception e)
        {
            throw new Exception("Error parsing CSV file : " +e.getMessage());
        }

        List<UserCsvRecord> usersToProcess = new ArrayList<>();
        List<Map<String,String>> invalidEntries = new ArrayList<>();
        //to make sure emails cannot be duplicate in the csv file
        Set<String> emailsInCsv = new HashSet<>();

        AtomicInteger rowIndex = new AtomicInteger(1);
        for(UserCsvRecord record : allRecords)
        {
            Map<String,String> error = validateRecord(record, emailsInCsv , rowIndex.get() , adminRole);
            if(!error.isEmpty())
                invalidEntries.add(error);
            else {
                usersToProcess.add(record);
                emailsInCsv.add(record.getEmail().toLowerCase());
            }
            rowIndex.getAndIncrement();
        }
        if(!usersToProcess.isEmpty())
        {
            Set<String> uniqueEmails = usersToProcess.stream()
                    .map(r -> r.getEmail().toLowerCase())
                    .collect(Collectors.toSet());
            Set<String> existingEmails = userRepository.findExistingEmails(new ArrayList<>(uniqueEmails));
            usersToProcess.removeIf(record -> {
                if(existingEmails.contains(record.getEmail().toLowerCase()))
                {
                    invalidEntries.add(Map.of(
                            "Line",String.valueOf(rowIndex.getAndIncrement()),
                            "Email", record.getEmail(),
                            "Error" , "Email already exists in the system."
                    ));
                    return true;
                }
                return  false;
            });
        }
        if (!invalidEntries.isEmpty()) {
            return ImportResult.builder()
                    .status("partial success")
                    .message(invalidEntries.size()+"errors. Some essential fields are missing or duplicated. No users were created.")
                    .errorCount(invalidEntries.size())
                    .errorDetails(invalidEntries)
                    .build();
        }
        if(usersToProcess.isEmpty())
        {
            return ImportResult.builder()
                    .status("Failure")
                    .message("No valid users found to import.")
                    .errorCount(0)
                    .build();
        }
         return createUsersAndSendInvites(usersToProcess , adminId);
    }


    private Map<String, String> validateRecord(UserCsvRecord record, Set<String> emailsInCsv, int rowIndex , Role adminRole) {
        List<String> errors = new ArrayList<>();
        String emailNormalized = record.getEmail() != null ? record.getEmail().trim().toLowerCase() : null;
        if (record.getName() == null || record.getName().isBlank() ||
                record.getRole() == null || record.getRole().isBlank() ||
                record.getEmail() == null || record.getEmail().isBlank()) {
            errors.add("One or more essential fields (Name, Role, Email) are missing.");
        }
        if (errors.isEmpty() && record.getRole() != null) {
            try {
                Role targetRole = Role.valueOf(record.getRole().toUpperCase().trim());

                if (adminRole == Role.ADMIN && targetRole != Role.EDUCATOR) {
                    // ADMIN can ONLY create EDUCATORs
                    errors.add("ADMIN can only create users with the role 'EDUCATOR'.");
                } else if (adminRole == Role.SUPERADMIN && targetRole != Role.ADMIN && targetRole != Role.EDUCATOR) {
                    // SUPERADMIN can create ADMIN or EDUCATOR
                    errors.add("SUPERADMIN can only create users with roles 'ADMIN' or 'EDUCATOR'.");
                }
                else if (targetRole == Role.SUPERADMIN) {
                    errors.add("Cannot create users with the role 'SUPERADMIN' via bulk import.");
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid role value: '" + record.getRole() + "'.");
            }
        }

        if (emailNormalized != null && emailsInCsv.contains(emailNormalized)) {
            errors.add("Email is duplicated within the uploaded file.");
        }

        if(errors.isEmpty())
            return Collections.emptyMap();
        else{
            return Map.of(
                    "Line",String.valueOf(rowIndex),
                    "Email" , record.getEmail() != null ? record.getEmail() : "MISSING",
                    "Error" , String.join("|" , errors)
            );
        }
    }


    private ImportResult createUsersAndSendInvites(List<UserCsvRecord> records , UUID adminId) {
        Role adminRole = userRepository.findRoleById(adminId)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found for ID: " + adminId));
        if (adminRole != Role.ADMIN && adminRole != Role.SUPERADMIN) {
            throw new IllegalStateException("Unauthorized: Only authorized admins can create users.");
        }
        List<Map<String, String>> failedInvites = new ArrayList<>();
        List<User> newUsers = new ArrayList<>();
        for (UserCsvRecord record : records) {
            String token = UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusDays(7);
            Role targetRole = Role.valueOf(record.getRole());
            if (!helper.isAuthorizedToCreate(adminRole, targetRole)) {
                log.warn("Admin {} (Role: {}) attempted to create unauthorized role: {}", adminId, adminRole, targetRole);
                throw new SecurityException("Admin role " + adminRole + " is not authorized to create users with role " + targetRole);
            }
            else{

                 User user =
                         User.builder()
                                 .name(record.getName())
                                 .role(Role.valueOf(record.getRole().toString()))
                                 .email(record.getEmail().toLowerCase())
                                 .status(Status.INVITED)
                                 .createdBy(adminId)
                                 .updatedBy(adminId)
                                 .invitationToken(token)
                                 .tokenExpiresAt(expirationTime)
                                 .build();
                 User newUser = userRepository.save(user);
                 newUsers.add(user);
                 UserInvitedEvent event = new UserInvitedEvent(user.getId().toString(),record.getName(),record.getEmail().toLowerCase().trim(),token,Instant.now().toString());
                 try{
                 userInvitedEventProducer.publishUserInvitedEvent(event);
                log.info("Successfully sent invitation mail to {} with email {}", record.getName(), record.getEmail());
                 }
                 catch (Exception e){
                     log.error("Failed to send invitation mail to {} with email {}",record.getName(),record.getEmail());
                     failedInvites.add(Map.of(record.getName(),record.getEmail()));
                     userRepository.delete(user);

                 }

            }
            }
        return ImportResult
                .builder()
                .status("Success with the creation of "+newUsers.size()+" new users and invitation failed for "+failedInvites.size()+" users")
                .message("Bulk Import completed. Invitation emails sent to " +newUsers.size()+ " users.")
                .processedCount(newUsers.size())
                .errorCount(failedInvites.size())
                .errorDetails(failedInvites)
                .build();

    }

    public Role findRoleById(UUID adminId)
    {
        Optional<Role> role = userRepository.findRoleById(adminId);
        if(role.isEmpty())
            throw new RuntimeException("Role for the adminId : "+adminId+ " not found");
        else
            return role.get();
    }

    public Set<String> findUnregisteredEmails(Set<String> allEmails, UUID adminId)
    {
        Set<String> registeredEmails;
        try {
            registeredEmails = userRepository.findRegisteredEmailsByAdmin(adminId, allEmails);
            System.out.println("userService -> registered emails  : "+registeredEmails);
        }
        catch (Exception e)
        {
            log.error("Could not find the registered emails");
            throw new RuntimeException("ERROR retrieving the registered emails");
        }

        Set<String> unregisteredEmails = new HashSet<>(allEmails);
        unregisteredEmails.removeAll(registeredEmails);
        return unregisteredEmails;
    }

    public List<UUID> getIdsByEmails(List<String> emails){

        List<UUID> Ids = new ArrayList<>();
        try{
            Ids =  userRepository.getIdsByEmails(emails);
        }
        catch (Exception e)
        {
            log.error("Could not find the Ids for the  emails");
            throw new RuntimeException("ERROR retrieving the Ids for emails");
        }
        return Ids;
    }



}
