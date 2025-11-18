package com.appShala.userGroupService.Payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class GroupImportResult {
    private String status;
    private String message;
    private int processedCount;
    private int errorCount;
    private int registeredUsers;
    private int unRegisteredUsers;

    private List<Map<String , String>> errorDetails;
}
