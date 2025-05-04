package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserTagWeightId;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureUpdateService {

    private static final int MAX_EVENTS = 20;

    @Autowired private MyJdbcService              jdbc;
    @Autowired private UserTagWeightRepository    tagWeightRepo;
    @Autowired private UserPlayEventRepository    playRepo;

    private final Map<String, double[]> tagCache  = new HashMap<>();
    private final Map<String, Deque<Integer>> playCache = new HashMap<>();
    private final Map<String, List<UserTagWeight>> pendingUpdates = new HashMap<>();

    public void recordFeedback(String userId, int songId, long timestamp, boolean isFavorite, boolean isUnfavorited) {
        double actualPct = Math.min(1.0, timestamp / (double) jdbc.getSongDuration(songId));
        System.out.printf("[recordFeedback] userId=%s, songId=%d, timestamp=%d, isFavorite=%s, isUnfavorited=%s%n",
                userId, songId, timestamp, isFavorite, isUnfavorited);

        savePlayEvent(userId, songId);

        List<Tag> tags = jdbc.getSongWithTags(songId).tags;
        List<UserTagWeight> weights = tagWeightRepo.findByIdNumber(userId);
        Map<Integer, UserTagWeight> weightMap = new HashMap<>();
        for (UserTagWeight w : weights) {
            weightMap.put(w.getTagId(), w);
        }

        double positiveDelta = 0.07;
        double negativeDelta = 0.01;

        for (Tag tag : tags) {
            UserTagWeight w = weightMap.getOrDefault(tag.getTagId(), new UserTagWeight(userId, tag.getTagId(), 0.2));
            double old = w.getWeight();
            double newWeight = old;

            if (isFavorite || actualPct >= 0.8) {
                newWeight += positiveDelta;
            } else if (actualPct <= 0.4 || isUnfavorited) {
                newWeight -= negativeDelta;
            }

            newWeight = Math.min(1.0, Math.max(0.05, newWeight));
            w.setWeight(newWeight);
            weightMap.put(tag.getTagId(), w);
        }

        pendingUpdates.computeIfAbsent(userId, k -> new ArrayList<>()).addAll(weightMap.values());
    }

    @Transactional
    public void flushToDb() {
        System.out.println("[flushToDb] Flushing data to DB...");
        int count = 0;
        for (Map.Entry<String, List<UserTagWeight>> entry : pendingUpdates.entrySet()) {
            tagWeightRepo.saveAll(entry.getValue());
            count += entry.getValue().size();
        }
        pendingUpdates.clear();
        System.out.printf("[flushToDb] Saved %d weights and %d events.%n",
                count, playCache.values().stream().mapToInt(Deque::size).sum());
    }

    private void savePlayEvent(String userId, int songId) {
        playCache.putIfAbsent(userId, new ArrayDeque<>());
        Deque<Integer> q = playCache.get(userId);

        List<UserPlayEvent> existing = playRepo.findAllByUserId(userId);
        Set<Integer> existingSongIds = existing.stream()
                .map(UserPlayEvent::getSongId)
                .collect(Collectors.toSet());

        if (existingSongIds.contains(songId)) {
            existing.stream()
                    .filter(e -> e.getSongId() == songId)
                    .findFirst()
                    .ifPresent(playRepo::delete);
            q.remove(songId);
        }

        if (q.size() == MAX_EVENTS) {
            Integer oldest = q.pollFirst();
            existing.stream()
                    .filter(e -> e.getSongId() == oldest)
                    .findFirst()
                    .ifPresent(playRepo::delete);
        }

        q.addLast(songId);

        UserPlayEvent ev = new UserPlayEvent();
        ev.setUserId(userId);
        ev.setSongId(songId);
        ev.setSongTitle(jdbc.getSongTitle(songId));

        long offset = q.size();
        long millisOffset = offset * 1000L + new Random().nextInt(1000);
        ev.setPlayTime(new Timestamp(System.currentTimeMillis() + millisOffset));

        playRepo.save(ev);
    }

    private UserTagWeight getOrCreateWeight(String userId, int tagId) {
        UserTagWeightId key = new UserTagWeightId(userId, tagId);
        Optional<UserTagWeight> opt = tagWeightRepo.findById(key);
        return opt.orElse(new UserTagWeight(userId, tagId, 0.2));
    }

    public void normalizeWeights(String userId) {
        List<UserTagWeight> weights = tagWeightRepo.findByIdNumber(userId);
        double sum = weights.stream().mapToDouble(UserTagWeight::getWeight).sum();
        for (UserTagWeight w : weights) {
            double normalized = w.getWeight() / sum;
            w.setWeight(normalized);
        }
        tagWeightRepo.saveAll(weights);
    }
}
