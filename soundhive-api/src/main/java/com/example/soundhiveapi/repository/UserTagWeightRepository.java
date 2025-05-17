package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.model.UserTagWeightId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagWeightRepository extends JpaRepository<UserTagWeight, UserTagWeightId> {

    /**
     * Retrieve all tag weights for a specific user.
     * @param idNumber the user's ID number
     * @return list of UserTagWeight entries
     */
    List<UserTagWeight> findByIdNumber(String idNumber);

    /**
     * Fetch a list of all distinct user IDs that have tag weights.
     * Useful for iterating over all active users.
     */
    @Query("SELECT DISTINCT utw.idNumber FROM UserTagWeight utw")
    List<String> findDistinctUserIds();

    /**
     * Delete all tag weights for a user — typically used when clearing data.
     * @param idNumber the user's ID
     */
    void deleteByIdNumber(String idNumber); // used when flushing play history
}
