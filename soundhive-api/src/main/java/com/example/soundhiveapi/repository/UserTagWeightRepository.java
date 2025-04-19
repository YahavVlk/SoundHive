package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.model.UserTagWeightId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserTagWeightRepository extends JpaRepository<UserTagWeight, UserTagWeightId> {

    // Use the property "idNumber" to query based on your schema.
    List<UserTagWeight> findByIdNumber(String idNumber);

    // Retrieve distinct idNumber values from the entity.
    @Query("SELECT DISTINCT utw.idNumber FROM UserTagWeight utw")
    List<String> findDistinctUserIds();
}
