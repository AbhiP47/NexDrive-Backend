package com.appShala.userGroupService.client;

import com.appShala.userGroupService.Payload.GroupCsvRecord;
import com.appShala.userGroupService.Payload.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(name="USERSERVICE" , path="/api/user")
public interface UserServiceClient {

    @GetMapping("/userExistsById/{userId}")
    public boolean userExistById(@PathVariable("userId") UUID userId);

    @PostMapping("/registerUsers-groupImport")
    public List<UserDTO> registerUsers(@RequestBody List<GroupCsvRecord> users ,@RequestParam("adminId") UUID adminId);

    @GetMapping("/findRoleById")
    public String findRoleByUserId(@RequestParam("adminId") UUID adminId);

    @GetMapping("/find-unregistered-emails")
    public Set<String> findUnRegisteredEmails(@RequestParam("allEmails") List<String> emailsInRecord ,@RequestParam("adminId") UUID adminId);

    @GetMapping("/getIdsByEmails")
    public List<UUID> findIdsByEmails(@RequestParam("emails") List<String> emails);

}
