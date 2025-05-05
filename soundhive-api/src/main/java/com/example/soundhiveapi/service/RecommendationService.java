package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private UserPlayEventRepository playRepo;

    private final Set<String> coldStartUsers = new HashSet<>();
    private final Map<String, Deque<Integer>> sessionHistory = new ConcurrentHashMap<>();

    public void startSession(String userId) {
        Deque<Integer> recent = playRepo.findTop20ByUserIdOrderByPlayTimeDesc(userId).stream()
                .map(UserPlayEvent::getSongId)
                .limit(10)
                .collect(Collectors.toCollection(ArrayDeque::new));
        sessionHistory.put(userId, recent);
    }

    public void endSession(String userId) {
        sessionHistory.remove(userId);
    }

    public List<Song> recommend(String userId, int k, Set<Integer> recentlyPlayed) {
        List<Song> recommended;

        boolean isCold = !playRepo.existsByUserId(userId) || coldStartUsers.contains(userId);

        if (isCold) {
            recommended = recommendCollaboratively(userId, k, recentlyPlayed);
            if (recommended.size() < k) {
                coldStartUsers.remove(userId);
                List<Song> fallback = recommendNeurally(userId, k - recommended.size(), recentlyPlayed, recommended);
                recommended.addAll(fallback);
            } else {
                coldStartUsers.add(userId);
            }
        } else {
            recommended = recommendNeurally(userId, k, recentlyPlayed, new ArrayList<>());
        }

        Deque<Integer> history = sessionHistory.computeIfAbsent(userId, u -> new ArrayDeque<>());
        for (Song s : recommended) {
            history.remove(s.getSongId());
            history.addLast(s.getSongId());
            if (history.size() > 20) history.pollFirst();
        }

        return recommended;
    }

    public List<Song> recommend(String userId, int k) {
        return recommend(userId, k, getRecentPlays(userId));
    }

    private List<Song> recommendCollaboratively(String userId, int k, Set<Integer> recentlyPlayed) {
        System.out.println("? Recommending using Collaborative Filtering (first time)");
        List<Song> batch = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>(jdbc.getCollaborativeCandidates(userId));
        Collections.shuffle(candidates);

        Set<Integer> exclude = new HashSet<>(recentlyPlayed);
        exclude.addAll(sessionHistory.getOrDefault(userId, new ArrayDeque<>()));

        for (Integer songId : candidates) {
            if (exclude.contains(songId)) continue;
            Song s = jdbc.getSongById(songId);
            if (s != null && !containsSong(batch, s.getSongId())) {
                batch.add(s);
            }
            if (batch.size() == k) break;
        }

        return batch;
    }

    private List<Song> recommendNeurally(String userId, int k, Set<Integer> recentlyPlayed, List<Song> alreadyRecommended) {
        System.out.println("? Recommending using Neural Network");
        List<Song> batch = new ArrayList<>();

        System.out.println("[Debug] Input tag weights for user " + userId + ": " + Arrays.toString(jdbc.getUserTagWeightsArray(userId)));
        double[] scores = jdbc.getPredictedScores(userId);
        List<Integer> allSongIds = jdbc.getDistinctSongIds();

        Set<Integer> excluded = new HashSet<>(recentlyPlayed);
        alreadyRecommended.forEach(s -> excluded.add(s.getSongId()));
        excluded.addAll(sessionHistory.getOrDefault(userId, new ArrayDeque<>()));

        final double rarityWeightFactor = 0.25;
        PriorityQueue<SongScore> heap = new PriorityQueue<>((a, b) -> Double.compare(b.score, a.score));

        for (int i = 0; i < allSongIds.size(); i++) {
            int sid = allSongIds.get(i);
            if (excluded.contains(sid)) continue;
            Song song = jdbc.getSongById(sid);
            if (song == null) continue;

            MyJdbcService.SongWithTags swt = jdbc.getSongWithTags(sid);
            if (swt == null || swt.tags.isEmpty()) continue;

            List<Tag> songTags = swt.tags;
            Map<Integer, Double> userWeights = new HashMap<>();
            for (Tag tag : songTags) {
                double weight = jdbc.getTagWeightsForUser(userId, Collections.singletonList(tag)).get(0);
                userWeights.put(tag.getTagId(), weight);
            }

            Map<Tag, Double> rawMap = jdbc.getTagGlobalPopularity(songTags);
            Map<Integer, Double> globalPopularity = new HashMap<>();
            for (Tag tag : songTags) {
                globalPopularity.put(tag.getTagId(), rawMap.getOrDefault(tag, 0.0));
            }

            double rarityBonus = 0;
            for (Tag tag : songTags) {
                double rarity = globalPopularity.containsKey(tag.getTagId())
                        ? 1.0 / Math.max(1, globalPopularity.get(tag.getTagId()))
                        : 0.0;
                double userWeight = userWeights.getOrDefault(tag.getTagId(), 0.0);
                rarityBonus += rarity * userWeight;
            }

            double finalScore = scores[i] + rarityWeightFactor * rarityBonus;
            heap.add(new SongScore(sid, finalScore));
        }

        while (!heap.isEmpty() && batch.size() < k) {
            SongScore top = heap.poll();
            Song s = jdbc.getSongById(top.songId);
            if (s != null && !containsSong(batch, s.getSongId())) {
                batch.add(s);
            }
        }

        return batch;
    }

    private Set<Integer> getRecentPlays(String userId) {
        List<UserPlayEvent> allEvents = jdbc.getUserPlayHistory(userId);
        allEvents.sort(Comparator.comparing(UserPlayEvent::getPlayTime).reversed());

        return allEvents.stream()
                .map(UserPlayEvent::getSongId)
                .distinct()
                .limit(20)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsSong(List<Song> list, int songId) {
        for (Song s : list) {
            if (s.getSongId() == songId) return true;
        }
        return false;
    }

    private static class SongScore {
        int songId;
        double score;
        SongScore(int id, double s) {
            songId = id; score = s;
        }
    }
}
