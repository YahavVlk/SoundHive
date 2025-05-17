package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.*;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureUpdateService {

    private static final int MAX_EVENTS = 20; // Limit for play history stored per user
    private static final int BATCH_SIZE = 5;   // Flush when at least 5 tag updates collected

    @Autowired private MyJdbcService jdbc;
    @Autowired private UserTagWeightRepository tagWeightRepo;
    @Autowired private UserPlayEventRepository playRepo;
    @Autowired private TrainingService trainingService;

    // In-memory caches
    private final Map<String, List<UserTagWeight>> pendingUpdates = new HashMap<>();
    private final Map<String, Integer> updateCounter = new HashMap<>();

    /**
     * Records user feedback and defers writing to DB until batch size met.
     */
    public void recordFeedback(String userId, int songId, long timestamp, boolean isFavorite, boolean isUnfavorited) {
        double actualPct = Math.min(1.0, timestamp / (double) jdbc.getSongDuration(songId));
        System.out.printf("[recordFeedback] userId=%s, songId=%d, timestamp=%d, isFavorite=%s, isUnfavorited=%s%n",
                userId, songId, timestamp, isFavorite, isUnfavorited);

        savePlayEvent(userId, songId);
        List<Tag> tags = jdbc.getSongWithTags(songId).tags;
        List<UserTagWeight> weights = tagWeightRepo.findByIdNumber(userId);

        Map<Integer, UserTagWeight> weightMap = new HashMap<>();
        for (UserTagWeight w : weights) weightMap.put(w.getTagId(), w);

        double positiveDelta = 0.07;
        double negativeDelta = 0.01;

        for (Tag tag : tags) {
            UserTagWeight w = weightMap.getOrDefault(tag.getTagId(), new UserTagWeight(userId, tag.getTagId(), 0.2));
            double old = w.getWeight();
            double newWeight = old;

            if (isFavorite || actualPct >= 0.8) newWeight += positiveDelta;
            else if (actualPct <= 0.4 || isUnfavorited) newWeight -= negativeDelta;

            w.setWeight(Math.min(1.0, Math.max(0.05, newWeight)));
            weightMap.put(tag.getTagId(), w);
        }

        List<UserTagWeight> updates = new ArrayList<>(weightMap.values());
        pendingUpdates.computeIfAbsent(userId, k -> new ArrayList<>()).addAll(updates);
        updateCounter.put(userId, updateCounter.getOrDefault(userId, 0) + 1);

        if (updateCounter.get(userId) >= BATCH_SIZE) {
            flushUserToDb(userId);
        }
    }

    /**
     * Flushes cached updates for a specific user.
     */
    @Transactional
    public void flushUserToDb(String userId) {
        List<UserTagWeight> list = pendingUpdates.getOrDefault(userId, new ArrayList<>());
        if (!list.isEmpty()) {
            tagWeightRepo.saveAll(list);
        }
        pendingUpdates.remove(userId);
        updateCounter.remove(userId);
    }

    /**
     * Flushes all cached updates.
     */
    @Transactional
    public void flushToDb() {
        System.out.println("[flushToDb] Flushing all data to DB...");
        for (String userId : new HashSet<>(pendingUpdates.keySet())) {
            flushUserToDb(userId);
        }
    }

    /**
     * Saves play event and trims history to MAX_EVENTS.
     */
    private void savePlayEvent(String userId, int songId) {
        Timestamp playTime = new Timestamp(System.currentTimeMillis());

        UserPlayEventId id = new UserPlayEventId(userId, songId, playTime);
        UserPlayEvent ev = new UserPlayEvent(id, false, false);
        ev.setTitle(jdbc.getSongTitle(songId));

        playRepo.save(ev);

        List<UserPlayEvent> events = playRepo.findAllByIdUserId(userId);
        if (events.size() > MAX_EVENTS) {
            events.sort(Comparator.comparing(e -> e.getId().getPlayTime()));
            playRepo.deleteAll(events.subList(0, events.size() - MAX_EVENTS));
        }
    }


    /**
     * Optional normalization helper.
     */
    public void normalizeWeights(String userId) {
        List<UserTagWeight> weights = tagWeightRepo.findByIdNumber(userId);
        double sum = weights.stream().mapToDouble(UserTagWeight::getWeight).sum();
        for (UserTagWeight w : weights) {
            w.setWeight(w.getWeight() / sum);
        }
        tagWeightRepo.saveAll(weights);
    }
}