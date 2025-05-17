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
    private final Map<String, Set<Integer>> recentHistory = new ConcurrentHashMap<>();

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
        recentHistory.put(userId, recentlyPlayed);

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

                System.out.println("▶ Listening: " + song.getTitle());

                for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                    if (!activeUsers.contains(userId)) break;

                    long timestamp = (long)(duration * pct);
                    actual = pct;

                    double skipChance = 0.5 - 0.4 * pct;
                    boolean skip = isSimulated && Math.random() < skipChance;
                    boolean fav = isSimulated && !skip && Math.random() < 0.1;
                    boolean unfav = isSimulated && !skip && !fav && Math.random() < 0.03;

                    featureSvc.recordFeedback(userId, songId, timestamp, fav, unfav);
                    System.out.println((skip ? "⏭ Skipped" : fav ? "❤️ Favorited" : unfav ? "🗑 Unfavorited" : "🎧 Listened") + " at " + (int)(pct * 100) + "%");

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
                Map<Integer, Double> label = new HashMap<>();
                label.put(songId, actual);
                trainer.trainOnExample(new TrainingExample(input, label));
            }

            System.out.println("Listening stopped for userId=" + userId);
        }).start();
    }

    public void stopListening(String userId) {
        System.out.println("[ListeningService] Stop requested for user: " + userId);
        activeUsers.remove(userId);
        userQueues.remove(userId);
        simulationMode.remove(userId);
        recentHistory.remove(userId);
        trainer.saveModel();
    }

    public Song getNextRecommendedSong(String userId) {
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        recentHistory.putIfAbsent(userId, new LinkedHashSet<>(jdbc.getUserPlayEvents(userId).stream().map(Song::getSongId).toList()));

        Deque<Song> queue = userQueues.get(userId);
        Set<Integer> recentlyPlayed = recentHistory.get(userId);

        if (queue.size() <= REFILL_THRESHOLD) {
            List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
            queue.addAll(batch);
        }

        return queue.peekFirst(); // Let frontend decide how to respond
    }

    public void commitManualFeedback(String userId, boolean favorite, boolean unfavorite) {
        Deque<Song> queue = userQueues.get(userId);
        Set<Integer> recentlyPlayed = recentHistory.get(userId);

        if (queue == null || queue.isEmpty()) return;

        Song song = queue.pollFirst();
        if (song == null) return;

        int songId = song.getSongId();
        long timestamp = System.currentTimeMillis();

        featureSvc.recordFeedback(userId, songId, timestamp, favorite, unfavorite);
        featureSvc.flushToDb();

        recentlyPlayed.add(songId);
        if (recentlyPlayed.size() > 20) {
            Iterator<Integer> it = recentlyPlayed.iterator();
            it.next();
            it.remove();
        }

        double[] input = jdbc.getUserTagWeightsArray(userId);
        Map<Integer, Double> label = new HashMap<>();
        label.put(songId, 1.0);
        trainer.trainOnExample(new TrainingExample(input, label));
    }
}
