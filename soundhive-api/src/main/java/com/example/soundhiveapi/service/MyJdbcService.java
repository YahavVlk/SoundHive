package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.*;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired private SongRepository songRepo;

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
        List<UserPlayEvent> events = userPlayEventRepository.findTop20ByIdUserIdOrderByIdPlayTimeDesc(userId);
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

        if (uw.isEmpty()) {
            Map<Tag, Double> popularity = getTagGlobalPopularity(tags);
            double total = popularity.values().stream().mapToDouble(Double::doubleValue).sum();
            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);
                double norm = total == 0 ? 1.0 / tags.size() : popularity.getOrDefault(tag, 0.0) / total;
                vector[i] = 0.1 + 0.2 * norm;
            }
            return vector;
        }

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
        for (Song s : songs) ids.add(s.getSongId());
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
        List<Integer> songIds = getDistinctSongIds();
        neuralNetwork.setSongIdOrder(songIds);
        double[] predictions = neuralNetwork.predict(input);
        int index = songIds.indexOf(songId);
        return index >= 0 && index < predictions.length ? predictions[index] : 0.0;
    }

    public double[] getPredictedScores(String userId) {
        double[] input = getUserTagWeightsArray(userId);
        List<Integer> songIds = getDistinctSongIds();
        neuralNetwork.setSongIdOrder(songIds);
        return neuralNetwork.predict(input);
    }

    public Map<Integer, Double> getPredictedScoreMap(String userId) {
        double[] scores = getPredictedScores(userId);
        List<Integer> songIds = neuralNetwork.getSongIdOrder();
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

        String raw = song.getTags();
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

    public List<Tag> getTop5TagsForUser(String userId) {
        List<UserTagWeight> weights = userTagWeightRepository.findByIdNumber(userId);
        weights.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        List<Integer> topIds = weights.stream()
                .limit(5)
                .map(UserTagWeight::getTagId)
                .toList();
        return tagRepository.findAllById(topIds);
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
        List<Song> songs = songRepository.findAll();
        List<Tag> userTopTags = getTop5TagsForUser(userId);

        for (Song song : songs) {
            if (song.getTags() == null) continue;
            String[] rawTags = song.getTags().split(",");
            Set<String> tagSet = Arrays.stream(rawTags).map(String::trim).collect(Collectors.toSet());

            for (Tag tag : userTopTags) {
                if (tagSet.contains(tag.getTagName())) {
                    result.add(song.getSongId());
                    break;
                }
            }
        }
        return result;
    }

    public List<UserPlayEvent> getUserPlayHistory(String userId) {
        return userPlayEventRepository.findAllByIdUserId(userId);
    }

    public Map<Tag, Double> getTagGlobalPopularity(List<Tag> tags) {
        Map<Tag, Double> map = new HashMap<>();
        List<Song> allSongs = songRepository.findAll();

        for (Tag tag : tags) {
            String tagName = tag.getTagName().toLowerCase();
            double count = 0;

            for (Song song : allSongs) {
                String raw = song.getTags();
                if (raw == null) continue;
                List<String> tagList = Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());

                if (tagList.contains(tagName)) {
                    count++;
                }
            }

            map.put(tag, count);
        }

        return map;
    }

    public Map<Integer, Integer> getTagFatigue(String userId) {
        Map<Integer, Integer> map = new HashMap<>();
        List<UserPlayEvent> plays = getUserPlayHistory(userId);
        for (UserPlayEvent e : plays) {
            SongWithTags s = getSongWithTags(e.getSongId());
            for (Tag t : s.tags) {
                map.put(t.getTagId(), map.getOrDefault(t.getTagId(), 0) + 1);
            }
        }
        return map;
    }

    public Song getNextRecommendedSong(String userId) {
        List<Integer> recentSongIds = getRecentSongIds(userId);
        List<Song> allSongs = songRepo.findAll();

        for (Song song : allSongs) {
            if (!recentSongIds.contains(song.getSongId())) {
                return song;
            }
        }
        return allSongs.isEmpty() ? null : allSongs.get(0);
    }

    public List<Integer> getRecentSongIds(String userId) {
        String query = "SELECT song_id FROM user_playevents WHERE user_id = ? ORDER BY play_time DESC LIMIT 50";
        List<Integer> songIds = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    songIds.add(rs.getInt("song_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songIds;
    }

    public Map<Integer, Double> getUserTagWeightMap(String userId) {
        List<UserTagWeight> weights = userTagWeightRepository.findByIdNumber(userId);
        Map<Integer, Double> map = new HashMap<>();
        for (UserTagWeight w : weights) {
            map.put(w.getTagId(), w.getWeight());
        }
        return map;
    }

    public Deque<Integer> getLastPlayedSongIds(String userId, int limit) {
        String query = "SELECT song_id FROM user_playevents WHERE user_id = ? ORDER BY play_time DESC LIMIT ?";
        Deque<Integer> songIds = new ArrayDeque<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    songIds.addLast(rs.getInt("song_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return songIds;
    }

    public double[] getSongTagVector(int songId, List<Tag> tags) {
        Set<Integer> songTagIds = getTagIdsForSong(songId);
        double[] vector = new double[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            if (songTagIds.contains(tags.get(i).getTagId())) {
                vector[i] = 1.0;
            } else {
                vector[i] = 0.0;
            }
        }
        return vector;
    }

    public Set<Integer> getTagIdsForSong(int songId) {
        String sql = "SELECT tag_id FROM song_tag WHERE song_id = ?";
        Set<Integer> tagIds = new HashSet<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, songId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tagIds.add(rs.getInt("tag_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tagIds;
    }
}