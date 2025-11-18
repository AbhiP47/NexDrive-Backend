package com.appShala.userGroupService.Controller;

import com.appShala.userGroupService.Enum.GroupSortBy;
import com.appShala.userGroupService.Enum.SortDirection;
import com.appShala.userGroupService.Payload.GroupImportResult;
import com.appShala.userGroupService.Payload.UserDTO;
import com.appShala.userGroupService.Payload.UserGroupRequest;
import com.appShala.userGroupService.Payload.UserGroupResponse;
import com.appShala.userGroupService.Service.UserGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/group")
public class UserGroupController {
    private UserGroupService userGroupService;

    public UserGroupController(UserGroupService userGroupService)
    {
        this.userGroupService = userGroupService;
    }


    @PostMapping("/createGroup")
    public ResponseEntity<UserGroupResponse> createGroup(@RequestBody UserGroupRequest groupRequest, @RequestHeader("adminId") UUID adminId)
    {
        if (groupRequest.getName() == null || groupRequest.getName().isEmpty() || adminId == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            UserGroupResponse response = userGroupService.createGroup(groupRequest , adminId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

@GetMapping("/group/names")
    public ResponseEntity<List<UserGroupResponse>> getGroupNamesBtIds(@RequestParam("groupId") List<UUID> grouoIds)
{
    List<UserGroupResponse> details = userGroupService.getGroupDetailsByIds(grouoIds);
    return ResponseEntity.ok(details);
}

@DeleteMapping("/deleteGroup/{groupId}")
    public ResponseEntity<Void> deleteGroupById(@PathVariable("groupId") UUID groupId)
{
    userGroupService.deleteGroup(groupId);
    return ResponseEntity.noContent().build();
}

@PutMapping("/updateGroup/{groupId}")
    public ResponseEntity<UserGroupResponse> updateGroupById(@PathVariable("groupId") UUID groupId , @RequestBody UserGroupRequest request , @RequestHeader UUID adminId)
{
    try{
        UserGroupResponse response = userGroupService.updateGroup(groupId , request , adminId);
        return  ResponseEntity.status(HttpStatus.OK).body(response);
    }
    catch(IllegalArgumentException e)
    {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
    catch(RuntimeException e)
    {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
    @GetMapping("/getGroups")
    public Page<UserGroupResponse> listGroups(
            @RequestParam(required = false) String userName,
            @RequestParam(defaultValue = "byName") GroupSortBy sortBy,
            @RequestParam(defaultValue = "A_TO_Z") SortDirection sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    )
    {
        return userGroupService.getGroups(userName,sortBy,sortDirection,page,size);
    }

    @PostMapping("/bulk-import/validate")
    public ResponseEntity<GroupImportResult> importGroupMembers(@RequestParam("file") MultipartFile file , @RequestHeader("adminId") UUID adminId)
    {
        if(file.isEmpty())
        {
            GroupImportResult result = GroupImportResult.builder()
                    .status("IMPORT FAILED")
                    .message("ERROR : FILE IS EMPTY")
                    .processedCount(0)
                    .build();
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        if(!("text/csv").equalsIgnoreCase(file.getContentType()) && !(file.getOriginalFilename().toLowerCase().endsWith(".csv")))
        {
            GroupImportResult result = GroupImportResult.builder()
                    .status("IMPORT FAILED")
                    .message("File type not supported , please upload a CSV file")
                    .processedCount(0)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
            try{
                GroupImportResult result = userGroupService.validateFile(file , adminId);
                return ResponseEntity.status(HttpStatus.OK).body(result);
            }
            catch (Exception e){
                log.info("Import Failed: " + e.getMessage());
                GroupImportResult result = GroupImportResult.builder()
                        .status("Failure")
                        .message("Internal server error during processing : " + e.getMessage())
                        .errorCount(0)
                        .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        }

        @PostMapping("bulk-import/execute")
        public ResponseEntity<GroupImportResult>  executeBulkImport(@RequestParam("file") MultipartFile file , @RequestHeader("adminId") UUID adminId ,  UserGroupRequest request , boolean registerFlag)
        {
            if(file.isEmpty())
            {
                GroupImportResult result = GroupImportResult.builder()
                        .status("IMPORT FAILED")
                        .message("ERROR : FILE IS EMPTY")
                        .processedCount(0)
                        .build();
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            if(!("text/csv").equalsIgnoreCase(file.getContentType()) && !(file.getOriginalFilename().toLowerCase().endsWith(".csv")))
            {
                GroupImportResult result = GroupImportResult.builder()
                        .status("IMPORT FAILED")
                        .message("File type not supported , please upload a CSV file")
                        .processedCount(0)
                        .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
            if(request.getName()==null || request.getName().isEmpty())
            {
                GroupImportResult result = GroupImportResult.builder()
                        .status("IMPORT FAILED")
                        .message("ERROR : group name is required")
                        .processedCount(0)
                        .build();
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            try{
                GroupImportResult result = userGroupService.processBulkImport(file , adminId ,request , registerFlag);
                return ResponseEntity.status(HttpStatus.OK).body(result);
            }
            catch (Exception e){
                log.info("Import Failed: " + e.getMessage());
                GroupImportResult result = GroupImportResult.builder()
                        .status("Failure")
                        .message("Internal server error during processing : " + e.getMessage())
                        .errorCount(0)
                        .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        }
        @PostMapping("/registerUsers")
        public ResponseEntity<Void> registerUsers(List<UserDTO> users)
        {
            return null;
        }

    // get groupId by group name for the user service
    @GetMapping("/groupId/{groupName}")
    public ResponseEntity<UUID> getGroupIdByName(@PathVariable("groupName") String groupName , @RequestHeader("adminId") UUID adminId)
    {
        log.info("get groupId by group name for the user service  TRIGGERED");
        UUID groupId = userGroupService.findGroupIdByName(groupName,adminId);
        return ResponseEntity.status(HttpStatus.FOUND).body(groupId);
    }
}
