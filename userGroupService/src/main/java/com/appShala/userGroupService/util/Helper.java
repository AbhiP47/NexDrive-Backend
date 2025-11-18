package com.appShala.userGroupService.util;

import com.appShala.userGroupService.Payload.GroupCsvRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class Helper {


    public Map<String, String> validateRecord(GroupCsvRecord record, AtomicInteger rowIndex, List<String> emailsInRecords, String adminRole) {
        List<String> errors = new ArrayList<>();
        if (record.getName() == null || record.getName().isBlank() ||
                record.getRole() == null || record.getRole().isBlank() ||
                record.getEmail() == null || record.getEmail().isBlank()) {
            errors.add("One or more essential fields (Name, Role, Email) are missing.");
        }
        if (errors.isEmpty() && record.getRole() != null) {
            try {
                String targetRole = record.getRole().toUpperCase().trim();

                if (adminRole == "ADMIN" && targetRole != "EDUCATOR") {
                    // ADMIN can ONLY create EDUCATORs
                    errors.add("ADMIN can only create users with the role 'EDUCATOR'.");
                } else if (adminRole == "SUPERADMIN" && targetRole != "ADMIN" && targetRole != "EDUCATOR") {
                    // SUPERADMIN can create ADMIN or EDUCATOR
                    errors.add("SUPERADMIN can only create users with roles 'ADMIN' or 'EDUCATOR'.");
                } else if (targetRole == "SUPERADMIN") {
                    errors.add("Cannot create users with the role 'SUPERADMIN' via bulk import.");
                }
            } catch (IllegalArgumentException e) {
                errors.add("Invalid role value: '" + record.getRole() + "'.");

            }
        }
            String emailNormalized = record.getEmail().toLowerCase().trim();
            if (emailNormalized != null && checkIfValueRepeats(emailsInRecords,emailNormalized)) {
                errors.add("Email is duplicated within the uploaded file.");
           }
            if (errors.isEmpty())
                return Collections.emptyMap();
            else {
                return Map.of(
                        "Line", String.valueOf(rowIndex),
                        "Email", record.getEmail() != null ? record.getEmail() : "MISSING",
                        "Error", String.join("|", errors)
                );
            }
        }

    private boolean checkIfValueRepeats(List<String> emailsInRecords , String target)
    {
        long count = emailsInRecords.stream()
                .filter(target::equals)
                .count();
        return count >= 2;
    }
    }