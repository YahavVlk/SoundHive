package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Applies per‑song feedback to user tag‑weights in memory and flushes them back to MySQL.
 */
@Service
public class FeatureUpdateService {

    private static final double ALPHA = 0.1;

    @Autowired private MyJdbcService              jdbc;
    @Autowired private UserTagWeightRepository   repo;      // UserTagWeightRepository
    @Autowired private SongRepository             songRepo;  // SongRepository

    // In‑memory cache: userId → tag‑vector
    private final Map<String,double[]> cache = new HashMap<>();


    public void recordFeedback(String userId,
                               int songId,
                               long timestamp,
                               boolean isFavorite) {
        // 1) load or init this user’s tag‑vector
        double[] w = cache.computeIfAbsent(
                userId,
                id -> jdbc.getUserTagWeightsArray(id).clone()
        );

        // 2) derive actual outcome
        double actual = isFavorite
                ? 1.0
                : (timestamp >= 0.2 * jdbc.getSongDuration(songId) ? 1.0 : 0.0);

        // 3) get predicted score
        double predicted = jdbc.getPredictedScore(userId, songId);

        // 4) compute residual
        double residual  = actual - predicted;

        // 5) load the Song plus its parsed tag list
        MyJdbcService.SongWithTags swt = jdbc.getSongWithTags(songId);
        if (swt == null) return;

        // 6) update each tag‑weight
        for (Tag t : swt.tags) {
            int idx = jdbc.getTagIndex(t.getTagId());
            if (idx < 0) continue;
            w[idx] = clamp(w[idx] + ALPHA * residual);
        }
    }

    public void flushToDb() {
        List<Integer> tagIds = jdbc.getDistinctTagIds();
        List<UserTagWeight> batch = new ArrayList<>();

        for (Map.Entry<String,double[]> e : cache.entrySet()) {
            String userId    = e.getKey();
            double[] weights = e.getValue();
            for (int i = 0; i < tagIds.size(); i++) {
                batch.add(new UserTagWeight(userId, tagIds.get(i), weights[i]));
            }
        }
        repo.saveAll(batch);
    }

    private double clamp(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
