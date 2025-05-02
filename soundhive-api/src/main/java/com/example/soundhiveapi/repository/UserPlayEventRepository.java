package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserPlayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlayEventRepository extends JpaRepository<UserPlayEvent, Long> {
    List<UserPlayEvent> findTop20ByUserIdOrderByPlayTimeDesc(String userId);
    void deleteByUserId(String userId);
}
