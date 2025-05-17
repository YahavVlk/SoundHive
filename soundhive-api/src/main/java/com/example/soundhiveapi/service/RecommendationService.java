package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private UserPlayEventRepository playRepo;

    private final Set<String> coldStartUsers = new HashSet<>();

    public List<Song> recommend(String userId, int k, Set<Integer> recentlyPlayed) {
        List<Song> result;

        boolean hasHistory = playRepo.existsByIdUserId(userId);
        if (!hasHistory) coldStartUsers.add(userId);

        if (coldStartUsers.contains(userId)) {
            result = recommendCollaboratively(userId, k, recentlyPlayed);

            if (result.size() < k) {
                coldStartUsers.remove(userId);
                Set<Integer> combinedExcluded = new HashSet<>(recentlyPlayed);
                result.forEach(song -> combinedExcluded.add(song.getSongId()));
                List<Song> fallback = recommendNeurally(userId, k - result.size(), combinedExcluded, result);
                result.addAll(fallback);
            }
        } else {
            result = recommendNeurally(userId, k, recentlyPlayed, new ArrayList<>());
        }

        return result;
    }

    public List<Song> recommend(String userId, int k) {
        return recommend(userId, k, getRecentPlays(userId));
    }

    public List<Song> recommendAll(String userId, Set<Integer> excluded) {
        double[] scores = jdbc.getPredictedScores(userId);
        List<Integer> songIds = jdbc.getDistinctSongIds();
        final double rarityWeightFactor = 0.4;

        PriorityQueue<SongScore> heap = new PriorityQueue<>(Comparator.comparingDouble(s -> -s.score));
        System.out.println("[RecommendAll] Excluded song IDs: " + excluded);

        for (int i = 0; i < songIds.size(); i++) {
            int songId = songIds.get(i);
            if (excluded.contains(songId)) continue;

            Song song = jdbc.getSongById(songId);
            if (song == null) {
                System.out.println("[RecommendAll] Skipping songId=" + songId + " because getSongById returned null.");
                continue;
            }

            MyJdbcService.SongWithTags swt = jdbc.getSongWithTags(songId);
            if (swt == null || swt.tags.isEmpty()) continue;

            Map<Tag, Double> globalPopularity = jdbc.getTagGlobalPopularity(swt.tags);
            boolean isLowConfidence = swt.tags.stream()
                    .allMatch(tag -> globalPopularity.getOrDefault(tag, 0.0) < 3); // Threshold = 3
            if (isLowConfidence) continue;

            Map<Integer, Double> userWeights = new HashMap<>();
            List<Double> weights = jdbc.getTagWeightsForUser(userId, swt.tags);
            for (int j = 0; j < swt.tags.size(); j++) {
                userWeights.put(swt.tags.get(j).getTagId(), weights.get(j));
            }

            double rarityBonus = swt.tags.stream()
                    .mapToDouble(tag -> {
                        double count = globalPopularity.getOrDefault(tag, 0.0);
                        double inverseFreq = 1.0 / Math.log(2 + count);
                        return inverseFreq * userWeights.getOrDefault(tag.getTagId(), 0.0);
                    })
                    .sum();

            double finalScore = scores[i] + rarityWeightFactor * rarityBonus;

            List<UserPlayEvent> userHistory = jdbc.getUserPlayHistory(userId);
            boolean seenBefore = userHistory.stream().anyMatch(ev -> ev.getSongId() == songId);
            int totalPlays = userHistory.size();

            // Conditional fatigue-based boost for early users
            if (totalPlays < 20) {
                Map<Integer, Integer> fatigueMap = jdbc.getTagFatigue(userId);
                double fatigueBoost = swt.tags.stream()
                        .filter(tag -> fatigueMap.getOrDefault(tag.getTagId(), 0) <= 2
                                && userWeights.getOrDefault(tag.getTagId(), 0.0) >= 0.3)
                        .mapToDouble(tag -> 0.05)
                        .sum();
                fatigueBoost = Math.min(0.25, fatigueBoost);
                finalScore += fatigueBoost;
            }

            if (!seenBefore && totalPlays < 20) {
                double boost = (20 - totalPlays) / 20.0;
                finalScore += 0.3 * boost;
            }

            System.out.printf("[RecommendAll] Candidate songId=%d, title='%s', score=%.4f%n",
                    songId, song.getTitle(), finalScore);

            heap.add(new SongScore(songId, finalScore));
        }

        List<Song> ranked = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        while (!heap.isEmpty() && ranked.size() < songIds.size()) {
            int songId = heap.poll().songId;
            if (seen.contains(songId)) continue;

            Song song = jdbc.getSongById(songId);
            if (song != null) {
                ranked.add(song);
                seen.add(songId);
            } else {
                System.out.println("[RecommendAll] WARNING: Failed to retrieve songId=" + songId + " from DB when building ranked list.");
            }
        }

        System.out.println("[RecommendAll] Final ranked list size: " + ranked.size());
        return ranked;
    }

    private List<Song> recommendCollaboratively(String userId, int k, Set<Integer> excluded) {
        List<Integer> candidates = new ArrayList<>(jdbc.getCollaborativeCandidates(userId));
        Collections.shuffle(candidates);

        List<Song> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        for (int id : candidates) {
            if (excluded.contains(id) || seen.contains(id)) continue;
            Song song = jdbc.getSongById(id);
            if (song != null) {
                result.add(song);
                seen.add(id);
            }
            if (result.size() == k) break;
        }

        if (result.size() < k) {
            int remaining = k - result.size();
            Set<Integer> allExcluded = new HashSet<>(excluded);
            result.forEach(s -> allExcluded.add(s.getSongId()));
            List<Song> neuralFallback = recommendNeurally(userId, remaining, allExcluded, result);
            result.addAll(neuralFallback);
        }

        return result;
    }

    private List<Song> recommendNeurally(String userId, int k, Set<Integer> excluded, List<Song> alreadyRecommended) {
        Set<Integer> allExcluded = new HashSet<>(excluded);
        for (Song s : alreadyRecommended) allExcluded.add(s.getSongId());

        return recommendAll(userId, allExcluded).stream()
                .limit(k)
                .collect(Collectors.toList());
    }

    public Set<Integer> getRecentPlays(String userId) {
        return jdbc.getUserPlayHistory(userId).stream()
                .sorted(Comparator.comparing(UserPlayEvent::getPlayTime).reversed())
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
        SongScore(int songId, double score) {
            this.songId = songId;
            this.score = score;
        }
    }
}