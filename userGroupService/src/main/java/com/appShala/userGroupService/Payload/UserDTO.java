package com.appShala.userGroupService.Payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String name;
    private String role;
    private String email;
    private UUID createdBy;

}
