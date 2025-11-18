package com.appShala.userGroupService.ServiceImpl;

import com.appShala.userGroupService.Payload.GroupCsvRecord;
import com.appShala.userGroupService.Payload.UserDTO;
import com.appShala.userGroupService.client.UserServiceClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceClientImpl {
    private  final  UserServiceClient userServiceClient;



    public UserServiceClientImpl(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }


    public List<UserDTO> RegisterUsers(List<GroupCsvRecord> users, UUID adminId) {
        try{
            List<UserDTO> registeredUsers = userServiceClient.registerUsers(users, adminId);
            return registeredUsers;
        }

        catch (FeignException e)
        {
            log.error("Failed to register the users for the group import");
            throw  new RuntimeException("User registration for group import failed : "+e.getMessage());
        }

    }

    public String findRoleById(UUID adminId) {
        try {
            return userServiceClient.findRoleByUserId(adminId);
        } catch (FeignException e) {
            log.error("Failed to retrieve role for adminId {} due to User Service error: {}", adminId, e.getMessage());

            throw new RuntimeException("Role service lookup failed.", e);
        }
    }

    public Set<String> findUnRegisteredEmails(List<String> allEmails , UUID createdBy) {
        try{
            return userServiceClient.findUnRegisteredEmails(allEmails,createdBy);
        }
        catch (FeignException e)
        {
            log.error("Failed to retrieve the unregistered emails");
            throw new RuntimeException("unregistered emails lookup failed");
        }
    }
    public List<UUID>  getIdsByEmails(List<String> emails)
    {
        try{
            return userServiceClient.findIdsByEmails(emails);
        }
        catch (FeignException e)
        {
            log.error("Failed to retrieve Ids by  emails");
            throw new RuntimeException("Failed to retrieve Ids by  emails");
        }
    }

}
