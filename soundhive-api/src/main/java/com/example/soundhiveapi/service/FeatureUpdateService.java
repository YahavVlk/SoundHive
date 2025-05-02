package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.Tag;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.model.UserTagWeight;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import com.example.soundhiveapi.repository.UserTagWeightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class FeatureUpdateService {

    private static final int MAX_EVENTS = 20;

    private final MyJdbcService jdbc;
    private final UserTagWeightRepository tagWeightRepo;
    private final SongRepository songRepo;
    private final UserPlayEventRepository playRepo;

    private final Map<String, double[]> tagCache  = new HashMap<>();
    private final Map<String, Deque<Integer>> playCache = new HashMap<>();

    @Autowired
    public FeatureUpdateService(
            MyJdbcService jdbc,
            UserTagWeightRepository tagWeightRepo,
            SongRepository songRepo,
            UserPlayEventRepository playRepo
    ) {
        this.jdbc = jdbc;
        this.tagWeightRepo = tagWeightRepo;
        this.songRepo = songRepo;
        this.playRepo = playRepo;
    }

    public void recordFeedback(String userId, int songId, long timestamp, boolean isFavorite) {
        double[] w = tagCache.computeIfAbsent(userId, id -> jdbc.getUserTagWeightsArray(id).clone());

        boolean liked = isFavorite || (timestamp >= 0.2 * jdbc.getSongDuration(songId));
        if (!liked) return;

        MyJdbcService.SongWithTags swt = jdbc.getSongWithTags(songId);
        if (swt == null) return;

        Set<Integer> boosted = new HashSet<>();
        for (Tag tag : swt.tags) {
            int idx = jdbc.getTagIndex(tag.getTagId());
            if (idx >= 0) {
                w[idx] = clamp(w[idx] + 0.05 * (1.0 - w[idx]));
                boosted.add(idx);
            }
        }

        for (int i = 0; i < w.length; i++) {
            if (!boosted.contains(i)) {
                w[i] = clamp(w[i] - 0.02 * (w[i] - 0.1));
            }
        }

        Deque<Integer> queue = playCache.computeIfAbsent(userId, id -> new ArrayDeque<>());
        if (!queue.contains(songId)) {
            queue.addLast(songId);
            if (queue.size() > MAX_EVENTS) {
                queue.removeFirst();
            }
        }
    }

    @Transactional
    public void flushToDb() {
        try {
            List<Integer> tagIds = jdbc.getDistinctTagIds();
            Map<Integer, String> songTitles = jdbc.getSongTitlesMap();
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Jerusalem"));

            List<UserTagWeight> weights = new ArrayList<>();
            List<UserPlayEvent> events = new ArrayList<>();

            for (Map.Entry<String, double[]> entry : tagCache.entrySet()) {
                String userId = entry.getKey();
                double[] vec = entry.getValue();

                for (int i = 0; i < tagIds.size(); i++) {
                    weights.add(new UserTagWeight(userId, tagIds.get(i), vec[i]));
                }

                playRepo.deleteByUserId(userId);

                int offset = 0;
                for (Integer sid : playCache.getOrDefault(userId, new ArrayDeque<>())) {
                    String title = songTitles.get(sid);
                    if (title == null) continue;

                    UserPlayEvent ev = new UserPlayEvent();
                    ev.setUserId(userId);
                    ev.setSongId(sid);
                    ev.setSongTitle(title);
                    ev.setPlayTime(Timestamp.valueOf(now.plusSeconds(offset++)));
                    events.add(ev);
                }
            }

            tagWeightRepo.saveAll(weights);
            playRepo.saveAll(events);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private double clamp(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
