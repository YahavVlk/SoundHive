package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.TrainingExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ListeningService {

    @Autowired private MyJdbcService         jdbc;
    @Autowired private FeatureUpdateService  featureSvc;
    @Autowired private TrainingService       trainer;
    @Autowired private RecommendationService recommendationService;

    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Song>> userQueues = new ConcurrentHashMap<>();
    private final Map<String, Boolean> simulationMode = new ConcurrentHashMap<>();

    private static final int QUEUE_SIZE = 10;
    private static final int REFILL_THRESHOLD = 5;

    public void startSimulation(String userId, double interval) {
        simulationMode.put(userId, true);
        startListening(userId, interval);
    }

    public void startManual(String userId, double interval) {
        simulationMode.put(userId, false);
        startListening(userId, interval);
    }

    private void startListening(String userId, double feedbackIntervalPct) {
        System.out.println("[ListeningService] Starting for userId=" + userId);

        Set<Integer> recentlyPlayed = new LinkedHashSet<>();
        jdbc.getUserPlayEvents(userId).forEach(song -> recentlyPlayed.add(song.getSongId()));

        activeUsers.add(userId);
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        Deque<Song> queue = userQueues.get(userId);

        boolean isSimulated = simulationMode.getOrDefault(userId, true);

        new Thread(() -> {
            while (activeUsers.contains(userId)) {
                if (queue.size() <= REFILL_THRESHOLD) {
                    List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
                    queue.addAll(batch);
                }

                if (queue.isEmpty()) break;
                Song song = queue.pollFirst();
                int songId = song.getSongId();
                long duration = jdbc.getSongDuration(songId);

                double predicted = jdbc.getPredictedScore(userId, songId);
                double actual = 0;

                System.out.println("\u25B6 Listening: " + song.getTitle());

                for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                    if (!activeUsers.contains(userId)) break;

                    long timestamp = (long)(duration * pct);
                    actual = pct;

                    // New skip logic: more likely to listen fully the later we are in the song
                    double skipChance = 0.5 - 0.4 * pct; // At 0% = 50%, at 80% = 18%, at 100% = 10%
                    boolean skip = isSimulated && Math.random() < skipChance;

                    boolean fav = isSimulated && !skip && Math.random() < 0.1;
                    boolean unfav = isSimulated && !skip && !fav && Math.random() < 0.03;

                    featureSvc.recordFeedback(userId, songId, timestamp, fav, unfav);
                    System.out.println((skip ? "\u23ED Skipped" : fav ? "\u2764\uFE0F Favorited" : unfav ? "\uD83D\uDDD1 Unfavorited" : "\uD83C\uDFA7 Listened") + " at " + (int)(pct * 100) + "%");

                    if (isSimulated) try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    if (skip) break;
                }

                recentlyPlayed.add(songId);
                if (recentlyPlayed.size() > 20) {
                    Iterator<Integer> it = recentlyPlayed.iterator();
                    it.next();
                    it.remove();
                }

                featureSvc.flushToDb();

                double[] input = jdbc.getUserTagWeightsArray(userId);
                double[] label = new double[jdbc.getDistinctSongIds().size()];
                int idx = jdbc.getDistinctSongIds().indexOf(songId);
                if (idx != -1) label[idx] = actual;

                trainer.trainOnExample(new TrainingExample(input, label));
            }
            System.out.println("\uD83D\uDED1 Listening stopped for userId=" + userId);
        }).start();
    }

    public void stopListening(String userId) {
        System.out.println("[ListeningService] Stop requested for user: " + userId);
        activeUsers.remove(userId);
        userQueues.remove(userId);
        simulationMode.remove(userId);
        trainer.saveModel();
    }
}
