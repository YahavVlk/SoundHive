package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RecommendationService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private UserPlayEventRepository playRepo;

    private final Set<String> coldStartUsers = new HashSet<>();

    /**
     * Recommends a batch of songs to the given user, avoiding recent plays.
     */
    public List<Song> recommend(String userId, int k, Set<Integer> recentlyPlayed) {
        List<Song> recommended;

        boolean isCold = !playRepo.existsByUserId(userId) || coldStartUsers.contains(userId);

        if (isCold) {
            recommended = recommendCollaboratively(userId, k, recentlyPlayed);
            if (recommended.size() < k) {
                coldStartUsers.remove(userId); // exit cold start if fallback needed
                List<Song> fallback = recommendNeurally(userId, k - recommended.size(), recentlyPlayed, recommended);
                recommended.addAll(fallback);
            } else {
                coldStartUsers.add(userId); // still in cold start phase
            }
        } else {
            recommended = recommendNeurally(userId, k, recentlyPlayed, new ArrayList<>());
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

        for (Integer songId : candidates) {
            if (recentlyPlayed.contains(songId)) continue;
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

        double[] scores = jdbc.getPredictedScores(userId);
        List<Integer> allSongIds = jdbc.getDistinctSongIds();
        Set<Integer> excluded = new HashSet<>(recentlyPlayed);
        alreadyRecommended.forEach(s -> excluded.add(s.getSongId()));

        PriorityQueue<SongScore> heap = new PriorityQueue<>((a, b) -> Double.compare(b.score, a.score));

        for (int i = 0; i < allSongIds.size(); i++) {
            int sid = allSongIds.get(i);
            if (excluded.contains(sid)) continue;
            heap.add(new SongScore(sid, scores[i]));
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
        List<Integer> list = jdbc.getUserPlayEvents(userId).stream()
                .map(Song::getSongId).toList();
        return new HashSet<>(list);
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
