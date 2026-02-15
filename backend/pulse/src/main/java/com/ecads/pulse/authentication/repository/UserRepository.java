package com.ecads.pulse.authentication.repository;

import com.ecads.pulse.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findUserByEmail(String email);
}
