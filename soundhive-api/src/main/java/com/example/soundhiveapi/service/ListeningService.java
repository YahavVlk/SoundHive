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
    private final Map<String, List<Song>> sessionHistory = new ConcurrentHashMap<>();

    private static final int QUEUE_SIZE = 10;
    private static final int REFILL_THRESHOLD = 5;
    private static final int HISTORY_LIMIT = 50;

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

        Deque<Integer> lastPlayed = jdbc.getLastPlayedSongIds(userId, HISTORY_LIMIT);
        Set<Integer> recentlyPlayed = new LinkedHashSet<>(lastPlayed);
        recentHistory.put(userId, recentlyPlayed);

        sessionHistory.put(userId, new ArrayList<>());

        activeUsers.add(userId);
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        Deque<Song> queue = userQueues.get(userId);

        boolean isSimulated = simulationMode.getOrDefault(userId, true);

        new Thread(() -> {
            while (activeUsers.contains(userId)) {
                if (queue.size() <= REFILL_THRESHOLD) {
                    List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
                    for (Song song : batch) {
                        if (song != null && !queue.stream().anyMatch(s -> s.getSongId() == song.getSongId())) {
                            queue.addLast(song);
                        }
                    }
                }

                if (queue.isEmpty()) break;
                Song song = queue.pollFirst();
                if (song == null) continue;

                int songId = song.getSongId();
                long duration = jdbc.getSongDuration(songId);

                double predicted = jdbc.getPredictedScore(userId, songId);
                double actual = 0;

                System.out.println("\u25B6 Listening: " + song.getTitle());

                boolean fav = false;
                boolean unfav = false;
                boolean skipped = false;

                for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                    if (!activeUsers.contains(userId)) break;

                    long timestamp = (long)(duration * pct);
                    actual = pct;

                    double skipChance = 0.5 - 0.4 * pct;
                    skipped = isSimulated && Math.random() < skipChance;

                    if (pct >= 0.8 && !fav && isSimulated && !skipped && Math.random() < 0.1) fav = true;
                    else if (pct >= 0.8 && !fav && !unfav && isSimulated && !skipped && Math.random() < 0.03) unfav = true;

                    System.out.println((skipped ? "\u23ED Skipped" : fav ? "\u2764\uFE0F Favorited" : unfav ? "\uD83D\uDDD1 Unfavorited" : "\uD83C\uDFA7 Listened") + " at " + (int)(pct * 100) + "%");

                    if (isSimulated) try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    if (skipped) break;
                }

                if (actual > 0.0) {
                    featureSvc.recordFeedback(userId, songId, (long)(duration * actual), fav, unfav);
                    featureSvc.flushToDb();

                    recentlyPlayed.add(songId);
                    if (recentlyPlayed.size() > HISTORY_LIMIT) {
                        Iterator<Integer> it = recentlyPlayed.iterator();
                        it.next();
                        it.remove();
                    }

                    double[] input = jdbc.getUserTagWeightsArray(userId);
                    Map<Integer, Double> label = new HashMap<>();
                    label.put(songId, actual);
                    trainer.trainOnExample(new TrainingExample(input, label));

                    sessionHistory.get(userId).add(song);
                }
            }

            System.out.println("\uD83D\uDED1 Listening stopped for userId=" + userId);
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
        recentHistory.putIfAbsent(userId, new LinkedHashSet<>(
                jdbc.getLastPlayedSongIds(userId, HISTORY_LIMIT)));

        Deque<Song> queue = userQueues.get(userId);
        Set<Integer> recentlyPlayed = recentHistory.get(userId);

        if (queue.size() <= REFILL_THRESHOLD) {
            List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
            for (Song song : batch) {
                if (song != null && !queue.stream().anyMatch(s -> s.getSongId() == song.getSongId())) {
                    queue.addLast(song);
                }
            }
        }

        return queue.peekFirst();
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
        if (recentlyPlayed.size() > HISTORY_LIMIT) {
            Iterator<Integer> it = recentlyPlayed.iterator();
            it.next();
            it.remove();
        }

        double[] input = jdbc.getUserTagWeightsArray(userId);
        Map<Integer, Double> label = new HashMap<>();
        label.put(songId, 1.0);
        trainer.trainOnExample(new TrainingExample(input, label));
    }

    public List<Song> getSessionHistoryForUser(String userId) {
        return sessionHistory.getOrDefault(userId, new ArrayList<>());
    }
}