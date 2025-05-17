package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.neural.TrainingExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ListeningService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private FeatureUpdateService featureSvc;
    @Autowired private TrainingService trainer;
    @Autowired private RecommendationService recommendationService;

    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, Deque<Song>> userQueues = new ConcurrentHashMap<>();
    private final Map<String, Boolean> simulationMode = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> recentHistory = new ConcurrentHashMap<>();
    private final Map<String, List<Song>> sessionHistory = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Double>> predictedScores = new ConcurrentHashMap<>();

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
        predictedScores.putIfAbsent(userId, new HashMap<>());

        Deque<Song> queue = userQueues.get(userId);
        boolean isSimulated = simulationMode.getOrDefault(userId, true);

        new Thread(() -> {
            while (activeUsers.contains(userId)) {
                if (queue.size() <= REFILL_THRESHOLD) {
                    List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
                    for (Song song : batch) {
                        if (song != null && queue.stream().noneMatch(s -> s.getSongId() == song.getSongId())) {
                            queue.addLast(song);
                            double predicted = jdbc.getPredictedScore(userId, song.getSongId());
                            predictedScores.get(userId).put(song.getSongId(), predicted);
                        }
                    }
                }

                if (queue.isEmpty()) break;
                Song song = queue.pollFirst();
                if (song == null) continue;

                int songId = song.getSongId();
                long duration = jdbc.getSongDuration(songId);
                double actual = 0;
                boolean fav = false;
                boolean skipped = false;

                System.out.println("▶ Listening: " + song.getTitle());

                for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                    if (!activeUsers.contains(userId)) break;

                    long timestamp = (long)(duration * pct);
                    actual = pct;

                    double baseSkip = 0.3;
                    double decayRate = 0.25;
                    double chanceToSkipNow = baseSkip * (1 - pct * decayRate);
                    chanceToSkipNow = Math.max(0.02, chanceToSkipNow);

                    // Slightly increased exploration with blended behavior
                    if (isSimulated && Math.random() < 0.25) {
                        boolean flip = Math.random() < 0.4;
                        if (flip) {
                            System.out.println("[Exploration] Light taste deviation at " + (int)(pct * 100) + "%");
                            chanceToSkipNow = 0.5 * chanceToSkipNow + 0.5 * (1.0 - chanceToSkipNow);
                        }
                    }

                    skipped = isSimulated && Math.random() < chanceToSkipNow;

                    if (pct >= 0.8 && !fav && isSimulated && !skipped && Math.random() < 0.1) {
                        fav = true;
                    }

                    System.out.println((skipped ? "⏭ Skipped" : fav ? "❤️ Favorited" : "🎧 Listened") + " at " + (int)(pct * 100) + "%");

                    if (isSimulated) try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    if (skipped) break;
                }

                if (actual > 0.0) {
                    boolean wasSkipped = actual < 1.0;

                    featureSvc.recordFeedback(userId, songId, (long)(duration * actual), fav, wasSkipped);
                    featureSvc.flushToDb();

                    recentlyPlayed.add(songId);
                    if (recentlyPlayed.size() > HISTORY_LIMIT) {
                        Iterator<Integer> it = recentlyPlayed.iterator();
                        it.next();
                        it.remove();
                    }

                    double predicted = predictedScores.getOrDefault(userId, Collections.emptyMap())
                            .getOrDefault(songId, 0.0);
                    double loss = Math.abs(predicted - actual);

                    if (actual < 0.2) {
                        System.out.printf("[TrainDecision] Skipped training — actual %.2f too low (likely noise).%n", actual);
                    } else if (loss > 0.15) {
                        double[] input = jdbc.getUserTagWeightsArray(userId);
                        Map<Integer, Double> label = Map.of(songId, actual);
                        trainer.trainOnExample(new TrainingExample(userId, input, label));
                    } else {
                        System.out.println("[TrainDecision] Skipped training for this song (low error).");
                    }

                    sessionHistory.get(userId).add(song);
                }
            }

            System.out.println("🛑 Listening stopped for userId=" + userId);
        }).start();
    }

    public void stopListening(String userId) {
        System.out.println("[ListeningService] Stop requested for user: " + userId);
        activeUsers.remove(userId);
        userQueues.remove(userId);
        simulationMode.remove(userId);
        recentHistory.remove(userId);
        predictedScores.remove(userId);
        trainer.saveModel();
    }

    public Song getNextRecommendedSong(String userId) {
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        recentHistory.putIfAbsent(userId, new LinkedHashSet<>(jdbc.getLastPlayedSongIds(userId, HISTORY_LIMIT)));

        Deque<Song> queue = userQueues.get(userId);
        Set<Integer> recentlyPlayed = recentHistory.get(userId);
        predictedScores.putIfAbsent(userId, new HashMap<>());

        if (queue.size() <= REFILL_THRESHOLD) {
            List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, recentlyPlayed);
            for (Song song : batch) {
                if (song != null && queue.stream().noneMatch(s -> s.getSongId() == song.getSongId())) {
                    queue.addLast(song);
                    double predicted = jdbc.getPredictedScore(userId, song.getSongId());
                    predictedScores.get(userId).put(song.getSongId(), predicted);
                }
            }
        }

        return queue.peekFirst();
    }

    public void commitManualFeedback(String userId, boolean favorite, boolean skipped) {
        Deque<Song> queue = userQueues.get(userId);
        Set<Integer> recentlyPlayed = recentHistory.get(userId);

        if (queue == null || queue.isEmpty()) return;
        Song song = queue.pollFirst();
        if (song == null) return;

        int songId = song.getSongId();
        long duration = jdbc.getSongDuration(songId);
        long timestamp = skipped ? (long)(duration * 0.4) : duration;
        double actual = skipped ? 0.4 : 1.0;

        featureSvc.recordFeedback(userId, songId, timestamp, favorite, skipped);
        featureSvc.flushToDb();

        recentlyPlayed.add(songId);
        if (recentlyPlayed.size() > HISTORY_LIMIT) {
            Iterator<Integer> it = recentlyPlayed.iterator();
            it.next();
            it.remove();
        }

        double predicted = predictedScores.getOrDefault(userId, Collections.emptyMap())
                .getOrDefault(songId, 0.0);
        double loss = Math.abs(predicted - actual);

        if (loss > 0.15) {
            double[] input = jdbc.getUserTagWeightsArray(userId);
            Map<Integer, Double> label = Map.of(songId, actual);
            trainer.trainOnExample(new TrainingExample(userId, input, label));
        }

        sessionHistory.getOrDefault(userId, new ArrayList<>()).add(song);
    }

    public List<Song> getSessionHistoryForUser(String userId) {
        return sessionHistory.getOrDefault(userId, new ArrayList<>());
    }
}