package com.example.soundhiveapi.repository;

import com.example.soundhiveapi.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {

    /**
     * Find a tag entity by its exact name.
     * @param tagName the name of the tag (must be unique)
     * @return Tag entity with the given name
     */
    Tag findByTagName(String tagName);

    /**
     * Retrieve all tags sorted by their tagId in ascending order.
     * Useful for maintaining consistent ordering (e.g., for network output).
     */
    List<Tag> findAllByOrderByTagIdAsc();
}
