package com.ecads.pulse.authentication.repository;

import com.ecads.pulse.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String uid);

    User getByUid(String uid);
}
