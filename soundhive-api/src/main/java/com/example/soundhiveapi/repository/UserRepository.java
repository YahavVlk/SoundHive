package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find a user by their email address.
     * @param email user's email
     * @return User entity (or null if not found)
     */
    User findByEmail(String email);

    /**
     * Check whether a user with the given ID number exists.
     * @param idNumber national ID string (primary key)
     * @return true if user exists, false otherwise
     */
    boolean existsById(String idNumber);
}
