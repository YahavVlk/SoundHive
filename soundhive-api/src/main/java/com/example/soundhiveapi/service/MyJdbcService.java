package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.repository.UserRepository;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.PriorityQueue;
import java.util.LinkedList;
import java.util.List;

@Service
public class MyJdbcService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private UserTagWeightRepository userTagWeightRepository;

    @Autowired
    private UserPlayEventRepository userPlayEventRepository;

    // Function 1: Get a user by email.
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Function 2: Check if the provided password matches the user's password.
    public boolean checkPassword(User user, String password) {
        return user != null && user.getPassword().equals(password);
    }

    // Function 3: Retrieve all tags.
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByTagIdAsc();
    }

    // Function 4: Retrieve the last 20 played songs for a user.
    public Queue<Song> getUserPlayEvents(String idNumber) {
        List<UserPlayEvent> events = userPlayEventRepository.findTop20ByUserIdOrderByPlayTimeDesc(idNumber);
        Queue<Song> songQueue = new LinkedList<>();
        for (UserPlayEvent event : events) {
            Song song = getSongById(event.getSongId());
            if (song != null) {
                songQueue.add(song);
            }
        }
        return songQueue;
    }

    // Function 5: Build the user x tag weights matrix using real data.
    public double[][] getUserTagWeightsMatrix() {
        List<String> userIds = getDistinctUserIds();
        List<Tag> tags = tagRepository.findAll();

        int numUsers = userIds.size();
        int numTags = tags.size();
        double[][] matrix = new double[numUsers][numTags];

        Map<Integer, Integer> tagIndexMap = new HashMap<>();
        for (int i = 0; i < tags.size(); i++) {
            tagIndexMap.put(tags.get(i).getTagId(), i);
        }

        List<UserTagWeight> weights = userTagWeightRepository.findAll();
        for (UserTagWeight w : weights) {
            int userIndex = userIds.indexOf(w.getIdNumber());
            Integer tagIndex = tagIndexMap.get(w.getTagId());
            if (userIndex >= 0 && tagIndex != null) {
                matrix[userIndex][tagIndex] = w.getWeight();
            }
        }
        return matrix;
    }

    // Function 6: Retrieve distinct user IDs from the user_tagweights table.
    public List<String> getDistinctUserIds() {
        return userTagWeightRepository.findDistinctUserIds();
    }

    // Function 7: Retrieve a max-heap of tag weights for a given user.
    public PriorityQueue<TagWeight> getUserTagMaxHeap(
            String idNumber,
            double[][] matrix,
            List<String> distinctUserIds,
            List<Tag> allTags
    ) {
        int rowIndex = distinctUserIds.indexOf(idNumber);
        PriorityQueue<TagWeight> maxHeap =
                new PriorityQueue<>((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        if (rowIndex == -1) {
            return maxHeap;
        }
        for (int i = 0; i < matrix[rowIndex].length; i++) {
            double weight = matrix[rowIndex][i];
            Tag tag = allTags.get(i);
            TagWeight tagWeight = new TagWeight(tag.getTagId(), tag.getTagName(), weight);
            maxHeap.add(tagWeight);
        }
        return maxHeap;
    }

     // Function 8: Returns a double[] vector of this user’s tag‑weights.
    public double[] getUserTagWeightsArray(String idNumber) {
        // 1. Get all tags (defines the vector length and order)
        List<Tag> tags = getAllTags();

        // 2. Prepare the result array, initialized to 0.0
        double[] weightsVector = new double[tags.size()];

        // 3. Load only this user’s tag‑weight rows
        List<UserTagWeight> userWeights = userTagWeightRepository.findByIdNumber(idNumber);

        // 4. For each record, find its index in the tags list and store the weight
        for (UserTagWeight w : userWeights) {
            int tagId = w.getTagId();
            double weight = w.getWeight();

            // scan tags list to find matching tagId
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).getTagId() == tagId) {
                    weightsVector[i] = weight;
                    break;  // stop inner loop once found
                }
            }
        }
        return weightsVector;
    }


    // Helper: Retrieve a Song by its ID.
    public Song getSongById(int songId) {
        return songRepository.findById(songId).orElse(null);
    }

    // Inner class for pairing tag IDs, names, and weights.
    public static class TagWeight {
        private int tagId;
        private String tagName;
        private double weight;

        public TagWeight(int tagId, String tagName, double weight) {
            this.tagId = tagId;
            this.tagName = tagName;
            this.weight = weight;
        }

        public int getTagId() { return tagId; }
        public String getTagName() { return tagName; }
        public double getWeight() { return weight; }

        @Override
        public String toString() {
            return "Tag " + tagName + " (ID: " + tagId + ", Weight: " + weight + ")";
        }
    }
}
