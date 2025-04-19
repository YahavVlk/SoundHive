package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Custom finder method to find by email
    User findByEmail(String email);
}
