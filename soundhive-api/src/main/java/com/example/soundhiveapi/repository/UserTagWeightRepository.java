package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.model.UserTagWeightId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagWeightRepository extends JpaRepository<UserTagWeight, UserTagWeightId> {

    List<UserTagWeight> findByIdNumber(String idNumber);

    @Query("SELECT DISTINCT utw.idNumber FROM UserTagWeight utw")
    List<String> findDistinctUserIds();

    void deleteByIdNumber(String idNumber);  // used when flushing play history
}
