package com.appshala.userService.Util;

import com.appshala.userService.Enum.Role;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class Helper {
    public boolean isAuthorizedToCreate(Role adminRole, Role targetRole) {
        // Prevent any admin from creating a SUPERADMIN
        if (targetRole == Role.SUPERADMIN) {
            return false;
        }

        if (adminRole == Role.SUPERADMIN) {
            // SUPERADMIN can create ADMIN or EDUCATOR
            return targetRole == Role.ADMIN || targetRole == Role.EDUCATOR;
        } else if (adminRole == Role.ADMIN) {
            // ADMIN can ONLY create EDUCATOR
            return targetRole == Role.EDUCATOR;
        }

        // Any other role (e.g., EDUCATOR, standard USER) cannot create users
        return false;
    }

    public String generateSecureToken()
    {
        return UUID.randomUUID().toString();
    }
}
