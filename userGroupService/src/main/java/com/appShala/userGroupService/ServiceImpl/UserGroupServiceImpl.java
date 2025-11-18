package com.appShala.userGroupService.ServiceImpl;

import com.appShala.userGroupService.Enum.GroupSortBy;
import com.appShala.userGroupService.Enum.SortDirection;
import com.appShala.userGroupService.Model.Membership;
import com.appShala.userGroupService.Model.UserGroup;
import com.appShala.userGroupService.Payload.*;
import com.appShala.userGroupService.Repository.MembershipRepository;
import com.appShala.userGroupService.Repository.UserGroupRepository;
import com.appShala.userGroupService.Service.UserGroupService;
import com.appShala.userGroupService.client.UserServiceClient;
import com.appShala.userGroupService.util.Helper;
import com.opencsv.bean.CsvToBeanBuilder;
import feign.FeignException;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final MembershipServiceImpl membershipService;
    private final MembershipRepository membershipRepository;
    private final Helper helper;
    private final UserServiceClientImpl userServiceClient;

    public UserGroupServiceImpl(UserGroupRepository userGroupRepository ,UserServiceClientImpl userServiceClient, MembershipServiceImpl membershipService , MembershipRepository membershipRepository, Helper helper) {
        this.userGroupRepository = userGroupRepository;
        this.membershipService = membershipService;
        this.membershipRepository = membershipRepository;
        this.userServiceClient=userServiceClient;
        this.helper = helper;
    }

    @Transactional
    @Override
    public UserGroupResponse createGroup(UserGroupRequest groupRequest , UUID adminId) {

        if(userGroupRepository.findByName(groupRequest.getName()).isPresent())
            throw new IllegalArgumentException("Group name already exists");

        UserGroup group = UserGroup.builder()
                .name(groupRequest.getName())
                .createdBy(adminId)
                .build();
        UserGroup savedGroup = userGroupRepository.save(group);

       List<Membership> memberships = membershipService.buildAndSaveInitialMemberships(
               savedGroup,
               savedGroup.getCreatedBy(),
               groupRequest.getInitialMembers()

       );
        UserGroupResponse groupResponse = UserGroupResponse.builder()
                .groupId(savedGroup.getId())
                .name(savedGroup.getName())
                .memberCount(memberships.size())
                .build();
        return groupResponse;
    }

    @Override
    public List<UserGroupResponse> getGroupDetailsByIds(List<UUID> groupIds) {
        if(groupIds == null || groupIds.isEmpty())
        {
            return Collections.emptyList();
        }
        List<UserGroup> groups = userGroupRepository.findAllById(groupIds);
        return groups.stream()
                .map(group -> new UserGroupResponse(group.getId(), group.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteGroup(UUID groupId) {
        if(userGroupRepository.existsById(groupId))
            throw new RuntimeException("Group not found");

        userGroupRepository.deleteById(groupId);
        membershipRepository.deleteByGroupId(groupId);
    }

    @Override
    @Transactional
    public UserGroupResponse updateGroup(UUID groupId, UserGroupRequest request, UUID adminId) {
        UserGroup group = userGroupRepository.findById(groupId).orElseThrow(()-> new RuntimeException(("Group not found")));
        if(request.getName() == null || request.getName().isEmpty())
        {
            throw new RuntimeException("Group name not provided");
        }
        if(userGroupRepository.findByName(request.getName()).isPresent())
            throw new RuntimeException("Group name already exists");
        group.setName(request.getName());
        UserGroup savedGroup = userGroupRepository.save(group);
        UserGroupResponse response = UserGroupResponse.builder()
                .groupId(groupId)
                .name(group.getName())
                .build();
        return response;
    }

    @Override
    public Page<UserGroupResponse> getGroups(String userName, GroupSortBy sortBy, SortDirection sortDirection, int page, int size) {
        Specification<UserGroup> spec = Specification.where(null);

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID findGroupIdByName(String groupName, UUID adminId) {
        Optional<UUID> groupId = userGroupRepository.findByNameAndCreatedBy(groupName,adminId);
        if(groupId.isEmpty())
            throw new RuntimeException("Group not found for the Group Name : "+groupName);
        else
        return groupId.get();
    }



    private Specification<UserGroup> groupsByMembership(List<UUID> userIds) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Membership> membershipRoot = subquery.from(Membership.class);

            subquery.select(membershipRoot.get("groupId")) // Select the Group ID from Membership table
                    .where(membershipRoot.get("userId").in(userIds)); // Where User ID is in the list

            // The main query includes the UserGroup only if its ID exists in the subquery result
            return cb.in(root.get("id")).value(subquery);
        };
    }

    @Override
    public GroupImportResult validateFile(MultipartFile file, UUID adminId) {
        String adminRole = userServiceClient.findRoleById(adminId);
        log.info("ADMIN role is : {}",adminRole);
        if(adminRole.equals("ADMIN") && adminRole.equals("SUPERADMIN")) {
            throw new RuntimeException("Unauthorized : This user does not have a valid  ROLE to create the group");
        }

        List<GroupCsvRecord> allRecords = new ArrayList<>();

        try(Reader reader = new InputStreamReader(file.getInputStream())){
            allRecords = new CsvToBeanBuilder<GroupCsvRecord>(reader)
                    .withType(GroupCsvRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing the CSV file "+e.getMessage());
        }

        List<String> emailsInRecords = allRecords.stream()
                .map(record->
                        record.getEmail().toLowerCase().trim()
                )
                .collect(Collectors.toList());
        Set<String> unRegisteredEmails = userServiceClient.findUnRegisteredEmails(emailsInRecords , adminId);
        System.out.println("Unregistered emails : "+unRegisteredEmails+"         Total is :"+unRegisteredEmails.size());

        return GroupImportResult.builder()
                .status("SUCCESS")
                .message("File validation successful")
                .registeredUsers(emailsInRecords.size()-unRegisteredEmails.size())
                .unRegisteredUsers(unRegisteredEmails.size())
                .build();
    }
    @Override
    public GroupImportResult processBulkImport(MultipartFile file, UUID adminId , UserGroupRequest request , boolean registerFlag) {
        String adminRole = userServiceClient.findRoleById(adminId);
        log.info("ADMIN role is : {}",adminRole);
        if(adminRole.equals("ADMIN") && adminRole.equals("SUPERADMIN")) {
            throw new RuntimeException("Unauthorized : This user does not have a valid  ROLE to create the group");
        }

        List<GroupCsvRecord> allRecords = new ArrayList<>();

        try(Reader reader = new InputStreamReader(file.getInputStream())){
            allRecords = new CsvToBeanBuilder<GroupCsvRecord>(reader)
                    .withType(GroupCsvRecord.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error parsing the CSV file "+e.getMessage());
        }

        List<GroupCsvRecord> usersToGroup = new ArrayList<>();
        List<GroupCsvRecord> usersToRegister = new ArrayList<>();
        List<Map<String,String>> invalidEntries = new ArrayList<>();

        List<String> emailsInRecords = allRecords.stream()
                .map(record->
                       record.getEmail().toLowerCase().trim()
                               )
                .collect(Collectors.toList());
        Set<String> unRegisteredEmails = userServiceClient.findUnRegisteredEmails(emailsInRecords , adminId);
        System.out.println("Unregistered emails : "+unRegisteredEmails+"         Total is :"+unRegisteredEmails.size());
        Integer numberOfUnregisteredRecords = unRegisteredEmails.size();
        if(numberOfUnregisteredRecords >=(0.25*(allRecords.size())) )
        {
            log.error("ERROR : Cannot create group as Most of the users are not registered");
            System.out.println(unRegisteredEmails);
            return GroupImportResult.builder()
                    .status("FAILED")
                    .message("ERROR : Cannot create group as Most of the users are not registered")
                    .processedCount(0)
                    .registeredUsers((allRecords.size())-numberOfUnregisteredRecords)
                    .unRegisteredUsers(numberOfUnregisteredRecords)
                    .build();
        }
        AtomicInteger rowIndex = new AtomicInteger(1);

        for(GroupCsvRecord record : allRecords)
        {
            Map<String ,String > error = helper.validateRecord(record,rowIndex,emailsInRecords,adminRole);
            if(!error.isEmpty()) {
                invalidEntries.add(error);
                System.out.println("Invalid entries are : "+invalidEntries);

            }

            else
            {
                if(unRegisteredEmails.contains(record.getEmail().toLowerCase().trim()))
                {
                    usersToRegister.add(record);
                    System.out.println("Users to register : "+usersToRegister);
                }
                else{
                    usersToGroup.add(record);
                    System.out.println("users to group : "+usersToGroup);
                }
            }
            rowIndex.getAndIncrement();
        }

        if(registerFlag)
        {
            try{
                registerUsersForGroup(usersToRegister,adminId);
            }
            catch (Exception e)
            {
                log.error("Error registering the users for group bulk-import "+e.getMessage());
            }
        }

        if(usersToGroup.isEmpty())
        {
            return GroupImportResult.builder()
                    .status("FAILED")
                    .message("No valid users found to import")
                    .build();
        }
        else{
            List<String> usersToGroupEmails = usersToGroup.stream()
                    .map(element ->
                            element.getEmail())
                    .collect(Collectors.toList());
            List<UUID> Ids = userServiceClient.getIdsByEmails(usersToGroupEmails);
            UserGroup group = UserGroup.builder()
                    .name(request.getName())
                    .createdBy(adminId)
                    .modifiedBy(adminId)
                    .build();
            System.out.println("group name : "+request.getName());
            UserGroup savedGroup = userGroupRepository.save(group);
            membershipService.buildAndSaveInitialMemberships(savedGroup,adminId,Ids);
            return GroupImportResult.builder()
                    .status("SUCCESS")
                    .message("Group bulk import successful")
                    .processedCount(Ids.size())
                    .registeredUsers(Ids.size())
                    .build();
        }
    }

    private void registerUsersForGroup(List<GroupCsvRecord> usersToRegister, UUID adminId) {
        userServiceClient.RegisterUsers(usersToRegister,adminId);
    }


}
