package com.appshala.userService.Repository;

import com.appshala.userService.Enum.Role;
import com.appshala.userService.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> , JpaSpecificationExecutor<User> {

    User findByEmail(String email);

    @Query("SELECT u.email FROM User u WHERE u.email IN :emails")
    Set<String> findExistingEmails(@Param("emails") List<String> emailList);

    @Query("SELECT u.role FROM User u WHERE u.id = :adminId")
    Optional<Role> findRoleById(@Param("adminId") UUID adminId);

    List<User> findAllByCreatedBy(UUID adminId);

    @Query("SELECT u.email FROM User u WHERE u.createdBy = :adminId AND u.email IN :emails")
    Set<String> findRegisteredEmailsByAdmin(
            @Param("adminId") UUID adminId,
            @Param("emails") Set<String> allEmails
    );

    @Query("SELECT u.id FROM User u WHERE u.email IN :emails")
    List<UUID> getIdsByEmails(@Param("emails") List<String> emails);
}