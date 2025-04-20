package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// TagRepository.java
@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    Tag findByTagName(String tagName);
    List<Tag> findAllByOrderByTagIdAsc();
}
