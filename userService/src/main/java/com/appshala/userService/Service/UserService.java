package com.appshala.userService.Service;

import com.appshala.userService.Enum.Role;
import com.appshala.userService.Enum.SortDirection;
import com.appshala.userService.Enum.Status;
import com.appshala.userService.Enum.UserSortBy;
import com.appshala.userService.Model.User;
import com.appshala.userService.Payloads.ImportResult;
import com.appshala.userService.Payloads.UserCreationRequest;
import com.appshala.userService.Payloads.UserRequest;
import com.appshala.userService.Payloads.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface UserService {
    public UserResponse createUser(UserCreationRequest userCreationRequest, UUID adminId);
    public User findByEmail(String email);
    public List<UserResponse> findAll(UUID adminId);
    public boolean existByEmail(String email);
    public Page<UserResponse> findUsers(
            Role role,
            Status status,
            String userGroupName,
            UserSortBy sortBy,
            SortDirection sortDirection,
            int page,
            int size,
            UUID adminId
    );
    public List<UserResponse> createUsers(List<UserCreationRequest> userCreationRequests, UUID adminId);
    public void deleteUserById(UUID id);
    public UserResponse updateUserById(UUID id , UserRequest userRequest);
    public List<UUID> getMemberIdsByGroupName(String groupName, UUID adminId);
    public UUID getCurrentAdminId(@RequestHeader("currentAdminId") UUID adminID);
    public boolean checkUserExistsById(UUID userId);

//    public ImportResult processBulkImport(MultipartFile file , UUID adminId) throws Exception;
}
