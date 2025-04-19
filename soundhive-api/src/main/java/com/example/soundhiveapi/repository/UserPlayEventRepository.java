package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.model.UserPlayEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserPlayEventRepository extends JpaRepository<UserPlayEvent, UserPlayEventId> {
    // Retrieves the last 20 events (ordered by play_time descending) for a given user.
    List<UserPlayEvent> findTop20ByUserIdOrderByPlayTimeDesc(String userId);
}
