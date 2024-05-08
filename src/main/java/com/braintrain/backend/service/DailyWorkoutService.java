package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.DailyWorkoutDto;
import com.braintrain.backend.entity.DailyWorkout;
import com.braintrain.backend.entity.GameType;
import com.braintrain.backend.entity.User;
import com.braintrain.backend.repository.DailyWorkoutRepository;
import com.braintrain.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Transactional
public class DailyWorkoutService {
    private final DailyWorkoutRepository dailyWorkoutRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ApiService apiService;

    @Scheduled(cron = "0 0 0 * * *")
    public void createNewTrack() {
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            DailyWorkout newWorkout = DailyWorkout.builder()
                    .isMemoryPlayed(false)
                    .isAttentionPlayed(false)
                    .isLanguagePlayed(false)
                    .isMathPlayed(false)
                    .createdDate(LocalDate.now())
                    .user(user)
                    .build();
            dailyWorkoutRepository.save(newWorkout);
        }
    }

    public ResponseEntity<Api<DailyWorkoutDto>> getTodayTrack() {
        User user = authService.getCurrentUser();
        Optional<DailyWorkout> todayTrackOptional = dailyWorkoutRepository.findByUserAndCreatedDate(
                user,
                LocalDate.now()
        );
        return todayTrackOptional.map(dailyWorkout -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(apiService.generateSuccessApi(mapToDto(dailyWorkout))))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(apiService.generateApi(
                            "today_tracking_not_found",
                            "Có lỗi xảy ra, xin hãy vào lại sau",
                            null
                )));
    }

    public ResponseEntity<Api<DailyWorkoutDto>> updateTodayTrack(double sleepHrs, int mood) {
        try {
            User user = authService.getCurrentUser();
            LocalDate today = LocalDate.now();
            Optional<DailyWorkout> todayTrackOptional = dailyWorkoutRepository.findByUserAndCreatedDate(user, today);
            if (todayTrackOptional.isEmpty()) {
                DailyWorkout newWorkout = dailyWorkoutRepository.save(DailyWorkout.builder()
                        .sleepHrs(sleepHrs)
                        .mood(mood)
                        .isMemoryPlayed(false)
                        .isAttentionPlayed(false)
                        .isLanguagePlayed(false)
                        .isMathPlayed(false)
                        .createdDate(today)
                        .user(user)
                        .build()
                );

                return ResponseEntity.status(HttpStatus.OK)
                        .body(apiService.generateSuccessApi(mapToDto(newWorkout)));
            } else {
                DailyWorkout todayTrack = todayTrackOptional.get();
                todayTrack.setSleepHrs(sleepHrs);
                todayTrack.setMood(mood);
                dailyWorkoutRepository.save(todayTrack);

                return ResponseEntity.status(HttpStatus.OK)
                        .body(apiService.generateSuccessApi(mapToDto(todayTrack)));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateApi(
                            "internal_server_error",
                            "Error: " + e.getMessage(),
                            null
                    ));
        }

    }

    public void completeOneDailyWorkout(GameType gameType) {
        User user = authService.getCurrentUser();
        Optional<DailyWorkout> todayTrackOptional = dailyWorkoutRepository.findByUserAndCreatedDate(
                user,
                LocalDate.now()
        );

        if (todayTrackOptional.isPresent()) {
            DailyWorkout todayTrack = todayTrackOptional.get();
            switch (gameType) {
                case MEMORY -> todayTrack.setMemoryPlayed(true);
                case ATTENTION -> todayTrack.setAttentionPlayed(true);
                case LANGUAGE -> todayTrack.setLanguagePlayed(true);
                case MATH -> todayTrack.setMathPlayed(true);
            }
            dailyWorkoutRepository.save(todayTrack);
        } else {
            dailyWorkoutRepository.save(DailyWorkout.builder()
                    .isMemoryPlayed(gameType == GameType.MEMORY)
                    .isAttentionPlayed(gameType == GameType.ATTENTION)
                    .isLanguagePlayed(gameType == GameType.LANGUAGE)
                    .isMathPlayed(gameType == GameType.MATH)
                    .createdDate(LocalDate.now())
                    .user(user)
                    .build()
            );
        }
    }

    void delete(User user) {
        dailyWorkoutRepository.deleteAllByUser(user);
    }

    private DailyWorkoutDto mapToDto(DailyWorkout dailyWorkout) {
        return DailyWorkoutDto.builder()
                .id(dailyWorkout.getId())
                .sleepHrs(dailyWorkout.getSleepHrs())
                .mood(dailyWorkout.getMood())
                .isMemoryPlayed(dailyWorkout.isMemoryPlayed())
                .isAttentionPlayed(dailyWorkout.isAttentionPlayed())
                .isLanguagePlayed(dailyWorkout.isLanguagePlayed())
                .isMathPlayed(dailyWorkout.isMathPlayed())
                .build();
    }
}
