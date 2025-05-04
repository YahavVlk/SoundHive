package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.*;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class MyJdbcService {

    private final DataSource dataSource;

    public MyJdbcService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private UserTagWeightRepository userTagWeightRepository;
    @Autowired private UserPlayEventRepository userPlayEventRepository;
    @Autowired private NeuralNetwork neuralNetwork;

    private final Map<Integer, Integer> songIndexCache = new HashMap<>();

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean checkPassword(User user, String password) {
        return user != null && user.getPassword().equals(password);
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByTagIdAsc();
    }

    public Song getSongById(int songId) {
        return songRepository.findById(songId).orElse(null);
    }

    public Queue<Song> getUserPlayEvents(String userId) {
        List<UserPlayEvent> events = userPlayEventRepository.findTop20ByUserIdOrderByPlayTimeDesc(userId);
        Queue<Song> queue = new LinkedList<>();
        for (UserPlayEvent e : events) {
            Song s = getSongById(e.getSongId());
            if (s != null) queue.add(s);
        }
        return queue;
    }

    public double[][] getUserTagWeightsMatrix() {
        List<String> userIds = getDistinctUserIds();
        List<Tag> tags = getAllTags();
        int U = userIds.size(), T = tags.size();
        double[][] matrix = new double[U][T];

        Map<Integer,Integer> tagIndex = new HashMap<>();
        for (int i = 0; i < T; i++) {
            tagIndex.put(tags.get(i).getTagId(), i);
        }

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

    public List<String> getDistinctUserIds() {
        return userTagWeightRepository.findDistinctUserIds();
    }

    public PriorityQueue<TagWeight> getUserTagMaxHeap(String userId, double[][] matrix, List<String> userIds, List<Tag> tags) {
        int row = userIds.indexOf(userId);
        PriorityQueue<TagWeight> heap = new PriorityQueue<>((a, b) -> Double.compare(b.weight, a.weight));
        if (row < 0) return heap;
        for (int i = 0; i < tags.size(); i++) {
            Tag t = tags.get(i);
            heap.add(new TagWeight(t.getTagId(), t.getTagName(), matrix[row][i]));
        }
        return heap;
    }

    public double[] getUserTagWeightsArray(String userId) {
        List<Tag> tags = getAllTags();
        double[] vector = new double[tags.size()];
        List<UserTagWeight> uw = userTagWeightRepository.findByIdNumber(userId);
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

    public List<Integer> getDistinctSongIds() {
        List<Song> songs = songRepository.findAll();
        List<Integer> ids = new ArrayList<>();
        for (int s = 0; s < songs.size(); s++) {
            int id = songs.get(s).getSongId();
            ids.add(id);
            songIndexCache.putIfAbsent(id, s);
        }
        return ids;
    }

    public List<Integer> getDistinctTagIds() {
        List<Tag> tags = getAllTags();
        List<Integer> ids = new ArrayList<>();
        for (Tag t : tags) ids.add(t.getTagId());
        return ids;
    }

    public int getTagIndex(int tagId) {
        List<Integer> ids = getDistinctTagIds();
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == tagId) return i;
        }
        return -1;
    }

    public double getPredictedScore(String userId, int songId) {
        double[] input = getUserTagWeightsArray(userId);
        double[] predictions = neuralNetwork.predict(input);
        Integer idx = songIndexCache.get(songId);
        if (idx == null) {
            List<Integer> all = getDistinctSongIds();
            idx = all.indexOf(songId);
            if (idx != -1) songIndexCache.put(songId, idx);
        }
        return idx != null && idx >= 0 && idx < predictions.length ? predictions[idx] : 0.0;
    }

    public double[] getPredictedScores(String userId) {
        double[] input = getUserTagWeightsArray(userId);
        return neuralNetwork.predict(input);
    }

    public Map<Integer, Double> getPredictedScoreMap(String userId) {
        double[] scores = getPredictedScores(userId);
        List<Integer> songIds = getDistinctSongIds();
        Map<Integer, Double> map = new HashMap<>();
        for (int i = 0; i < songIds.size(); i++) {
            map.put(songIds.get(i), scores[i]);
        }
        return map;
    }

    public long getSongDuration(int songId) {
        return 180_000;
    }

    public SongWithTags getSongWithTags(int songId) {
        Song song = getSongById(songId);
        if (song == null) return null;

        String raw = song.getRawTags();
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

    public static class SongWithTags {
        public final Song song;
        public final List<Tag> tags;
        public SongWithTags(Song song, List<Tag> tags) {
            this.song = song;
            this.tags = tags;
        }
    }

    public static class TagWeight {
        public final int tagId;
        public final String tagName;
        public final double weight;
        public TagWeight(int tagId, String tagName, double weight) {
            this.tagId = tagId;
            this.tagName = tagName;
            this.weight = weight;
        }
    }

    public List<Double> getTagWeightsForUser(String userId, List<Tag> tags) {
        double[] vector = getUserTagWeightsArray(userId);
        List<Double> weights = new ArrayList<>();
        for (Tag t : tags) {
            int idx = getTagIndex(t.getTagId());
            weights.add(idx >= 0 ? vector[idx] : 0.0);
        }
        return weights;
    }

    public Map<Integer, String> getSongTitlesMap() {
        String query = "SELECT song_id, title FROM songs";
        Map<Integer, String> map = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("song_id");
                String title = rs.getString("title");
                map.put(id, title);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return map;
    }

    public String getSongTitle(int songId) {
        String query = "SELECT title FROM songs WHERE song_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, songId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("title");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Title";
    }

    public Set<Integer> getCollaborativeCandidates(String userId) {
        Set<Integer> result = new HashSet<>();
        String sql = """
        SELECT DISTINCT sp.song_id
        FROM user_playevents sp
        JOIN (
            SELECT tag_id
            FROM user_tagweights
            WHERE id_number = ?
            ORDER BY weight DESC
            LIMIT 5
        ) AS top_tags ON EXISTS (
            SELECT 1
            FROM song_tags st
            WHERE st.song_id = sp.song_id
              AND st.tag_id = top_tags.tag_id
        )
        WHERE sp.user_id != ?
        LIMIT 50
    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt("song_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

}
