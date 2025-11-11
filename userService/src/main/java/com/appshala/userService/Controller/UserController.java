package com.appshala.userService.Controller;

import com.appshala.userService.Enum.Role;
import com.appshala.userService.Enum.SortDirection;
import com.appshala.userService.Enum.Status;
import com.appshala.userService.Enum.UserSortBy;
import com.appshala.userService.Payloads.ImportResult;
import com.appshala.userService.Payloads.UserCreationRequest;
import com.appshala.userService.Payloads.UserRequest;
import com.appshala.userService.Payloads.UserResponse;
import com.appshala.userService.Repository.UserRepository;
import com.appshala.userService.Service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
    }

    @PostMapping("/createUser")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreationRequest userCreationRequest, @RequestHeader("adminId") UUID adminId) {
        UserResponse userResponse = userService.createUser(userCreationRequest, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<UserResponse>> getUsersList(@RequestHeader("adminId") UUID adminId) {
        return ResponseEntity.ok(userService.findAll(adminId));
    }

    @GetMapping("/getUsers")
    public Page<UserResponse> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String userGroupName,
            @RequestParam(defaultValue = "byName") UserSortBy sortBy,
            @RequestParam(defaultValue = "A_TO_Z") SortDirection sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("adminId") UUID adminId
    ) {
        return userService.findUsers(role, status, userGroupName, sortBy, sortDirection, page, size, adminId);
    }

    @PostMapping("/createUsers")
    public ResponseEntity<List<UserResponse>> createUsers(@RequestBody List<UserCreationRequest> userCreationRequests, @RequestHeader("adminId") UUID adminId) {
        List<UserResponse> userResponses = userService.createUsers(userCreationRequests, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponses);
    }

    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<Void> deleteUserByEmail(@PathVariable UUID id) {
        try {
            userService.deleteUserById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/updateUser/{id}")
    public ResponseEntity<UserResponse> updateUserById(@PathVariable UUID id, @RequestBody UserRequest userRequest) {
        UserResponse userResponse = userService.updateUserById(id, userRequest);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

//    @PostMapping("/bulk-import-users")
//    public ResponseEntity<ImportResult> bulkImportUsers(@RequestParam("file")MultipartFile file , @RequestHeader("adminId") UUID adminId)
//    {
//        // Check if the file is empty
//        if(file.isEmpty()){
//            ImportResult importResult = ImportResult.builder()
//                    .status("Failure")
//                    .message("File is Empty")
//                    .errorCount(0)
//                    .build();
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(importResult);
//        }
//        // Basic MIME type check
//        if (!"text/csv".equalsIgnoreCase(file.getContentType()) &&
//                !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
//            ImportResult importResult = ImportResult.builder()
//                    .status("Failure")
//                    .message("Invalid file type. Please upload a CSV file.")
//                    .errorCount(0)
//                    .build();
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(importResult);
//        }
//        try{
//            ImportResult result = userService.processBulkImport(file , adminId);
//            return ResponseEntity.status(HttpStatus.OK).body(result);
//        }
//        catch (Exception e){
//            log.info("Import Failed: " + e.getMessage());
//            ImportResult result = ImportResult.builder()
//                    .status("Failure")
//                    .message("Internal server error during processing : " + e.getMessage())
//                    .errorCount(0)
//                    .build();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
//        }
//    }

    // for checking if the user exists by userId for the group service
    @GetMapping("/userExistsById/{userId}")
    public ResponseEntity<Boolean> checkUserExistsById(@PathVariable("userId") UUID userId) {
        boolean result = userService.checkUserExistsById(userId);
        return ResponseEntity.status(HttpStatus.FOUND).body(result);
    }
}
