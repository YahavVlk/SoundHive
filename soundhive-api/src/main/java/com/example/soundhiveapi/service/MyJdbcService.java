package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.User;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.TagRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import com.example.soundhiveapi.repository.UserRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * JDBC‑style service providing:
 *  • user/tag/song retrieval
 *  • user×tag matrix builders
 *  • feedback‑and‑prediction helpers
 *  • parsing of rawTags into real Tag objects
 */
@Service
public class MyJdbcService {

    @Autowired private UserRepository            userRepository;
    @Autowired private TagRepository             tagRepository;
    @Autowired private SongRepository            songRepository;
    @Autowired private UserTagWeightRepository   userTagWeightRepository;
    @Autowired private UserPlayEventRepository   userPlayEventRepository;
    @Autowired private NeuralNetwork             neuralNetwork;

    // 1) Find a user by email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // 2) Validate password
    public boolean checkPassword(User user, String password) {
        return user != null && user.getPassword().equals(password);
    }

    // 3) Return all tags ordered by tagId ascending
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByTagIdAsc();
    }

    // 4) Retrieve a Song by its ID
    public Song getSongById(int songId) {
        return songRepository.findById(songId).orElse(null);
    }

    // 5) Return last 20 played songs for a user
    public Queue<Song> getUserPlayEvents(String userId) {
        List<UserPlayEvent> events =
                userPlayEventRepository
                        .findTop20ByUserIdOrderByPlayTimeDesc(userId);
        Queue<Song> queue = new LinkedList<>();
        for (UserPlayEvent e : events) {
            Song s = getSongById(e.getSongId());
            if (s != null) queue.add(s);
        }
        return queue;
    }

    // 6) Build the user × tag weights matrix
    public double[][] getUserTagWeightsMatrix() {
        List<String> userIds = getDistinctUserIds();
        List<Tag>    tags    = getAllTags();
        int U = userIds.size(), T = tags.size();
        double[][] matrix = new double[U][T];

        // build tagId → column index
        Map<Integer,Integer> tagIndex = new HashMap<>();
        for (int i = 0; i < T; i++) {
            tagIndex.put(tags.get(i).getTagId(), i);
        }

        // fill matrix from all stored weights
        List<UserTagWeight> weights = userTagWeightRepository.findAll();
        for (UserTagWeight w : weights) {
            int uidx = userIds.indexOf(w.getIdNumber());
            Integer tidx = tagIndex.get(w.getTagId());
            if (uidx >= 0 && tidx != null) {
                matrix[uidx][tidx] = w.getWeight();
            }
        }
        return matrix;
    }

    // 7) Return list of all distinct user IDs
    public List<String> getDistinctUserIds() {
        return userTagWeightRepository.findDistinctUserIds();
    }

    // 8) For a given user, get a max‑heap of their tag weights
    public PriorityQueue<TagWeight> getUserTagMaxHeap(
            String userId,
            double[][] matrix,
            List<String> userIds,
            List<Tag> tags
    ) {
        int row = userIds.indexOf(userId);
        PriorityQueue<TagWeight> heap =
                new PriorityQueue<>((a,b) -> Double.compare(b.weight, a.weight));
        if (row < 0) return heap;
        for (int i = 0; i < tags.size(); i++) {
            Tag t = tags.get(i);
            heap.add(new TagWeight(t.getTagId(), t.getTagName(),
                    matrix[row][i]));
        }
        return heap;
    }

    // 9) Get a single user’s tag‑weights vector
    public double[] getUserTagWeightsArray(String userId) {
        List<Tag> tags = getAllTags();
        double[] vector = new double[tags.size()];
        List<UserTagWeight> uw = userTagWeightRepository
                .findByIdNumber(userId);
        for (UserTagWeight w : uw) {
            int tid = w.getTagId();
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).getTagId() == tid) {
                    vector[i] = w.getWeight();
                    break;
                }
            }
        }
        return vector;
    }

    // 10) Get all song IDs in stable order
    public List<Integer> getDistinctSongIds() {
        List<Song> songs = songRepository.findAll();
        List<Integer> ids = new ArrayList<>();
        for (Song s : songs) ids.add(s.getSongId());
        return ids;
    }

    // 11) Get all tag IDs in stable order
    public List<Integer> getDistinctTagIds() {
        List<Tag> tags = getAllTags();
        List<Integer> ids = new ArrayList<>();
        for (Tag t : tags) ids.add(t.getTagId());
        return ids;
    }

    // 12) Map a tagId to its index in the tag‑vector
    public int getTagIndex(int tagId) {
        List<Integer> ids = getDistinctTagIds();
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == tagId) return i;
        }
        return -1;
    }

    // 13) Predict score for (userId, songId)
    public double getPredictedScore(String userId, int songId) {
        double[] in     = getUserTagWeightsArray(userId);
        double[] scores = neuralNetwork.predict(in);
        List<Integer> sids = getDistinctSongIds();
        for (int i = 0; i < sids.size(); i++) {
            if (sids.get(i) == songId) return scores[i];
        }
        return 0.0;
    }

    // 14) Stub: song duration in ms
    public long getSongDuration(int songId) {
        return 180_000; // default 3 minutes
    }

    // 15) Load a Song plus parse its rawTags into real Tag objects
    public SongWithTags getSongWithTags(int songId) {
        Song song = getSongById(songId);
        if (song == null) return null;

        String raw = song.getRawTags(); // comma-separated, e.g. "Pop,Rock,..."
        List<Tag> list = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            String[] parts = raw.split(",");
            for (String name : parts) {
                Tag t = tagRepository.findByTagName(name.trim());
                if (t != null) list.add(t);
            }
        }
        return new SongWithTags(song, list);
    }

    /** Container for a Song plus its parsed Tag list. */
    public static class SongWithTags {
        public final Song      song;
        public final List<Tag> tags;
        public SongWithTags(Song song, List<Tag> tags) {
            this.song = song;
            this.tags = tags;
        }
    }

    /** Helper for max‑heap of tag weights. */
    public static class TagWeight {
        public final int    tagId;
        public final String tagName;
        public final double weight;
        public TagWeight(int tagId, String tagName, double weight) {
            this.tagId   = tagId;
            this.tagName = tagName;
            this.weight  = weight;
        }
    }
}
