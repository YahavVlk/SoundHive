package com.example.soundhiveapi.service;

import com.example.soundhiveapi.model.Song;
import com.example.soundhiveapi.model.UserPlayEvent;
import com.example.soundhiveapi.repository.SongRepository;
import com.example.soundhiveapi.repository.UserPlayEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ListeningService {

    @Autowired private MyJdbcService jdbc;
    @Autowired private SongRepository songRepo;
    @Autowired private UserPlayEventRepository playRepo;
    @Autowired private FeatureUpdateService featureUpdateService;

    private final Map<String, List<Integer>> sessionHistory = new HashMap<>();
    private final Set<String> simulationUsers = new HashSet<>();

    public Song getNextRecommendedSong(String userId) {
        return jdbc.getNextRecommendedSong(userId);
    }

    public void startSimulation(String userId) {
        simulationUsers.add(userId);
        sessionHistory.put(userId, new ArrayList<>());
    }

    public void stopSimulation(String userId) {
        simulationUsers.remove(userId);
        sessionHistory.remove(userId);
    }

    public boolean isSimulating(String userId) {
        return simulationUsers.contains(userId);
    }

    public void recordListening(String userId, int songId, int secondsListened) {
        Optional<Song> songOpt = songRepo.findById(songId);
        if (songOpt.isEmpty()) return;

        Song song = songOpt.get();
        int duration = (int) song.getDuration(); // Convert long to int
        double listenedPercent = (double) secondsListened / duration;
        double actualScore = Math.min(listenedPercent, 1.0);  // Real % listened

        UserPlayEvent event = new UserPlayEvent();
        event.setUserId(userId);
        event.setSongId(songId);
        event.setSongTitle(song.getTitle());
        playRepo.save(event);

        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
        featureUpdateService.recordFeedback(
                userId,
                songId,
                Timestamp.valueOf(LocalDateTime.now()).getTime(), // long timestamp
                actualScore > 0.8,                                // isFavorite
                actualScore < 0.2                                 // isUnfavorited
        );
        sessionHistory.computeIfAbsent(userId, k -> new ArrayList<>()).add(songId);
    }

    public List<Integer> getSessionHistory(String userId) {
        return sessionHistory.getOrDefault(userId, new ArrayList<>());
    }
}
