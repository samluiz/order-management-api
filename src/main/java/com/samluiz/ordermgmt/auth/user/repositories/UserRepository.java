package com.samluiz.ordermgmt.auth.user.repositories;

import com.samluiz.ordermgmt.auth.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
}
