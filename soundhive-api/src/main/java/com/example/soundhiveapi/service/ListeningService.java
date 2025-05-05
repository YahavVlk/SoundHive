package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.*;
import com.example.soundhiveapi.neural.TrainingExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
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
    private final Map<String, Deque<Integer>> sessionRecency = new ConcurrentHashMap<>();

    private static final int QUEUE_SIZE = 10;
    private static final int REFILL_THRESHOLD = 5;
    private static final int MIN_SONG_GAP = 10;
    private static final int EVAL_INTERVAL = 20;

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

        activeUsers.add(userId);
        userQueues.putIfAbsent(userId, new ArrayDeque<>());
        sessionRecency.putIfAbsent(userId, new ArrayDeque<>());

        Deque<Song> queue = userQueues.get(userId);
        Deque<Integer> recents = sessionRecency.get(userId);
        boolean isSimulated = simulationMode.getOrDefault(userId, true);

        class Counter {
            int totalSongs = 0;
            int hitCount   = 0;
        }
        Counter counter = new Counter();

        new Thread(() -> {
            while (activeUsers.contains(userId)) {
                if (queue.size() <= REFILL_THRESHOLD) {
                    List<Song> batch = recommendationService.recommend(userId, QUEUE_SIZE, new HashSet<>(recents));
                    queue.addAll(batch);
                }

                if (queue.isEmpty()) break;
                Song song = queue.pollFirst();
                int songId = song.getSongId();
                long duration = jdbc.getSongDuration(songId);

                double predicted = jdbc.getPredictedScore(userId, songId);
                double actual = 0;
                long finalTimestamp = 0;

                System.out.println("▶ Listening: " + song.getTitle());

                boolean skip = false;
                boolean fav = false;
                boolean unfav = false;

                List<Tag> songTags = jdbc.getSongWithTags(songId).tags;

                for (double pct = feedbackIntervalPct; pct <= 1.0; pct += feedbackIntervalPct) {
                    if (!activeUsers.contains(userId)) break;

                    finalTimestamp = (long)(duration * pct);
                    actual = pct;

                    List<Double> weights = jdbc.getTagWeightsForUser(userId, songTags);
                    double avgWeight = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0.2);

                    double skipChance  = 0.6 - avgWeight;
                    double favChance   = avgWeight - 0.6;
                    double unfavChance = 0.3 - avgWeight;

                    skip   = isSimulated && Math.random() < skipChance;
                    fav    = isSimulated && pct >= 0.8 && !skip && Math.random() < Math.max(0.0, favChance);
                    unfav  = isSimulated && pct <= 0.4 && !fav && Math.random() < Math.max(0.0, unfavChance);

                    System.out.println((skip ? "⏭ Skipped" : fav ? "❤️ Favorited" : unfav ? "🗑 Unfavorited" : "🎧 Listened") + " at " + (int)(pct * 100) + "%");

                    if (isSimulated) try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    if (skip) break;
                }

                if (finalTimestamp > 0) {
                    featureSvc.recordFeedback(userId, songId, finalTimestamp, fav, unfav);
                }

                if (!skip) {
                    recents.addLast(songId);
                    if (recents.size() > MIN_SONG_GAP) recents.pollFirst();
                }

                featureSvc.flushToDb();

                double[] input = jdbc.getUserTagWeightsArray(userId);
                Map<Integer, Double> labelMap = new HashMap<>();
                if (skip) {
                    labelMap.put(songId, 0.01);  // mild penalty
                } else {
                    labelMap.put(songId, actual);  // e.g., 0.4, 0.8, 1.0
                }

                trainer.trainOnExample(new TrainingExample(input, labelMap));

                counter.totalSongs++;
                if (predicted >= 0.5 && actual >= 0.8) {
                    counter.hitCount++;
                }

                if (counter.totalSongs % EVAL_INTERVAL == 0) {
                    double hitRate = (double) counter.hitCount / counter.totalSongs;
                    logHitRate(counter.totalSongs, hitRate);
                }
            }

            System.out.println("🔕 Listening stopped for userId=" + userId);
        }).start();
    }

    private void logHitRate(int total, double hitRate) {
        try (FileWriter writer = new FileWriter("hit_rate.csv", true)) {
            writer.write(total + "," + hitRate + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopListening(String userId) {
        System.out.println("[ListeningService] Stop requested for user: " + userId);
        activeUsers.remove(userId);
        userQueues.remove(userId);
        simulationMode.remove(userId);
        sessionRecency.remove(userId);

        System.out.println("[Debug] Forcing final training on remaining buffer");
        trainer.train();  // flush any untrained batch

        trainer.saveModel();
    }
}
