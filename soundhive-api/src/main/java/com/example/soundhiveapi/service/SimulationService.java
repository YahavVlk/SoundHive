package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.TrainingExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SimulationService {

    @Autowired private MyJdbcService         jdbc;
    @Autowired private FeatureUpdateService  featureSvc;
    @Autowired private TrainingService       trainer;
    @Autowired private RecommendationService recommendationService;

    /**
     * Simulate a listening session of `numSongs` songs for the given user,
     * issuing feedback every `feedbackIntervalPct` of the song’s duration.
     *
     * @param userId               the user's internal idNumber
     * @param numSongs             how many top songs to simulate
     * @param feedbackIntervalPct  fraction of song duration between feedback points (e.g. 0.2 for 20%)
     */
    public void simulateSession(String userId, int numSongs, double feedbackIntervalPct) {
        // 1) Get the top-N songs according to current recommendations
        List<Song> recommended = recommendationService.recommend(userId, numSongs);

        // 2) For each song, generate feedback
        for (Song song : recommended) {
            int songId = song.getSongId();
            long duration = jdbc.getSongDuration(songId);

            for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                long timestamp = (long)(duration * pct);
                double score   = jdbc.getPredictedScore(userId, songId);

                // Decide skip vs listen vs favorite
                boolean skip = Math.random() > score;
                boolean fav  = !skip && Math.random() < 0.1;

                // Record feedback
                featureSvc.recordFeedback(userId, songId, timestamp, fav);

                if (skip) break;  // move to next song on skip
            }
        }

        // 3) After simulating all, train on any remaining buffered examples
        trainer.train();
    }
}
