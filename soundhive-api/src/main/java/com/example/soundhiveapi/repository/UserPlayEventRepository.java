package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.model.UserPlayEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPlayEventRepository extends JpaRepository<UserPlayEvent, UserPlayEventId> {

    List<UserPlayEvent> findTop20ByIdUserIdOrderByIdPlayTimeDesc(String userId);

    List<UserPlayEvent> findByIdUserIdOrderByIdPlayTimeAsc(String userId);

    List<UserPlayEvent> findAllByIdUserId(String userId);

    boolean existsByIdUserId(String userId);

    void deleteByIdUserId(String userId);

    void deleteByIdUserIdAndIdSongId(String userId, int songId);
}
