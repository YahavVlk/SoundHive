package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.neural.NeuralNetwork;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import com.example.soundhiveapi.repository.UserRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Returns top‑K recommendations using either collaborative filtering or neural network.
 */
@Service
public class RecommendationService {

    private final MyJdbcService jdbcService;
    private final NeuralNetwork network;
    private final SongRepository songRepo;
    private final UserPlayEventRepository playRepo;
    private final UserTagWeightRepository tagWeightRepo;
    private final UserRepository userRepo;
    private final FeatureUpdateService featureUpdateService;

    @Autowired
    public RecommendationService(
            MyJdbcService jdbcService,
            NeuralNetwork network,
            SongRepository songRepo,
            UserPlayEventRepository playRepo,
            UserTagWeightRepository tagWeightRepo,
            UserRepository userRepo,
            FeatureUpdateService featureUpdateService
    ) {
        this.jdbcService = jdbcService;
        this.network = network;
        this.songRepo = songRepo;
        this.playRepo = playRepo;
        this.tagWeightRepo = tagWeightRepo;
        this.userRepo = userRepo;
        this.featureUpdateService = featureUpdateService;
    }

    public List<Song> recommend(String userId, int topK) {
        // Determine if the user has play history
        UserPlayEvent latestEvent = playRepo.findTop1ByUserIdOrderByPlayTimeDesc(userId);
        boolean hasHistory = latestEvent != null;

        System.out.println("Recommending using: " + (hasHistory ? "Neural Network" : "Collaborative Filtering"));

        if (hasHistory) {
            return recommendWithNeural(userId, topK);
        } else {
            return recommendCollaborative(userId, topK);
        }
    }

    private List<Song> recommendWithNeural(String userId, int topK) {
        double[] input = jdbcService.getUserTagWeightsArray(userId);
        double[] scores = network.predict(input);
        List<Integer> songIds = jdbcService.getDistinctSongIds();

        Set<Integer> recentlyPlayed = new HashSet<>();
        jdbcService.getUserPlayEvents(userId).forEach(song -> recentlyPlayed.add(song.getSongId()));

        List<Integer> sorted = new ArrayList<>(songIds);
        Collections.sort(sorted, Comparator.comparingDouble(a -> -scores[songIds.indexOf(a)]));

        List<Song> result = new ArrayList<>();
        for (int id : sorted) {
            if (recentlyPlayed.contains(id)) continue;
            Optional<Song> songOpt = songRepo.findById(id);
            songOpt.ifPresent(result::add);
            if (result.size() == topK) break;
        }
        return result;
    }

    private List<Song> recommendCollaborative(String userId, int topK) {
        double[][] matrix     = jdbcService.getUserTagWeightsMatrix();
        List<String> userIds  = jdbcService.getDistinctUserIds();
        List<Tag> tags        = jdbcService.getAllTags();
        PriorityQueue<MyJdbcService.TagWeight> heap = jdbcService.getUserTagMaxHeap(userId, matrix, userIds, tags);

        Set<Integer> top5Tags = new HashSet<>();
        for (int i = 0; i < 5 && !heap.isEmpty(); i++) {
            top5Tags.add(heap.poll().tagId);
        }

        Set<String> similarUsers = new HashSet<>();
        for (String uid : userIds) {
            PriorityQueue<MyJdbcService.TagWeight> h = jdbcService.getUserTagMaxHeap(uid, matrix, userIds, tags);
            int count = 0;
            int matched = 0;
            List<Integer> tagIds = new ArrayList<>();
            while (count++ < 5 && !h.isEmpty()) tagIds.add(h.poll().tagId);
            for (int tid : tagIds) if (top5Tags.contains(tid)) matched++;
            if (matched > 0) similarUsers.add(uid);
        }

        Map<Integer, Integer> freq = new HashMap<>();
        for (String uid : similarUsers) {
            jdbcService.getUserPlayEvents(uid).forEach(song ->
                    freq.put(song.getSongId(), freq.getOrDefault(song.getSongId(), 0) + 1));
        }

        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(freq.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        List<Song> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : sorted) {
            Optional<Song> s = songRepo.findById(e.getKey());
            s.ifPresent(result::add);
            if (result.size() == topK) break;
        }

        // Simulate feedback so the user has 20 songs in history and updated weights
        for (Song song : result) {
            int songId = song.getSongId();
            long duration = jdbcService.getSongDuration(songId);
            featureUpdateService.recordFeedback(userId, songId, duration, false);
        }
        featureUpdateService.flushToDb();
        return result;
    }

}