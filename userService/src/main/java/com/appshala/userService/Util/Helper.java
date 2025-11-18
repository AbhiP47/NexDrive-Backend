package com.appshala.userService.Util;

import com.appshala.userService.Enum.Role;
import com.appshala.userService.Model.User;
import com.appshala.userService.Payloads.UserResponse;
import com.appshala.userService.Repository.UserRepository;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class Helper {

    private final UserRepository userRepository;
    public Helper(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }
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

    public UserResponse convertToUserResponse(User savedUser) {
        return UserResponse.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .status(savedUser.getStatus())
                .build();
    }

}
