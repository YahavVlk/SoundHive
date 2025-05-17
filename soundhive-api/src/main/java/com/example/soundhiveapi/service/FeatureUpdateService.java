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

    private static final int MAX_EVENTS = 50;
    private static final int BATCH_SIZE = 5;

    @Autowired private MyJdbcService jdbc;
    @Autowired private UserTagWeightRepository tagWeightRepo;
    @Autowired private UserPlayEventRepository playRepo;
    @Autowired private TrainingService trainingService;

    private final Map<String, List<UserTagWeight>> pendingUpdates = new HashMap<>();
    private final Map<String, Integer> updateCounter = new HashMap<>();

    public void recordFeedback(String userId, int songId, long timestamp, boolean isFavorite, boolean isUnfavorited) {
        double actualPct = Math.min(1.0, timestamp / (double) jdbc.getSongDuration(songId));
        System.out.printf("[recordFeedback] userId=%s, songId=%d, timestamp=%d, isFavorite=%s, isUnfavorited=%s%n",
                userId, songId, timestamp, isFavorite, isUnfavorited);

        savePlayEvent(userId, songId);
        List<Tag> tags = jdbc.getSongWithTags(songId).tags;
        List<UserTagWeight> currentWeights = tagWeightRepo.findByIdNumber(userId);

        Map<Integer, UserTagWeight> weightMap = new HashMap<>();
        for (UserTagWeight w : currentWeights) {
            weightMap.put(w.getTagId(), new UserTagWeight(w.getIdNumber(), w.getTagId(), w.getWeight()));
        }

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

        List<UserTagWeight> changed = new ArrayList<>();
        for (UserTagWeight updated : weightMap.values()) {
            double original = currentWeights.stream()
                    .filter(w -> w.getTagId() == updated.getTagId())
                    .mapToDouble(UserTagWeight::getWeight)
                    .findFirst()
                    .orElse(0.2);

            if (Math.abs(original - updated.getWeight()) > 0.001) {
                changed.add(updated);
            }
        }

        pendingUpdates.computeIfAbsent(userId, k -> new ArrayList<>()).addAll(changed);
        updateCounter.put(userId, updateCounter.getOrDefault(userId, 0) + 1);

        synchronized (userId.intern()) {
            if (updateCounter.get(userId) >= BATCH_SIZE) {
                flushUserToDb(userId);
            }
        }
    }

    @Transactional
    public void flushUserToDb(String userId) {
        List<UserTagWeight> list = pendingUpdates.getOrDefault(userId, new ArrayList<>());
        if (!list.isEmpty()) {
            tagWeightRepo.saveAll(list);
        }
        pendingUpdates.remove(userId);
        updateCounter.remove(userId);
    }

    @Transactional
    public void flushToDb() {
        System.out.println("[flushToDb] Flushing all data to DB...");
        for (String userId : new HashSet<>(pendingUpdates.keySet())) {
            flushUserToDb(userId);
        }
    }

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

    public void normalizeWeights(String userId) {
        List<UserTagWeight> weights = tagWeightRepo.findByIdNumber(userId);
        double sum = weights.stream().mapToDouble(UserTagWeight::getWeight).sum();
        if (sum == 0) return;

        for (UserTagWeight w : weights) {
            w.setWeight(w.getWeight() / sum);
        }
        tagWeightRepo.saveAll(weights);
    }
}
