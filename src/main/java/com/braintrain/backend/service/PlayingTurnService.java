package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.PlayingTurnDto;
import com.braintrain.backend.entity.*;
import com.braintrain.backend.mapper.PlayingTurnMapper;
import com.braintrain.backend.repository.ObjectiveRepository;
import com.braintrain.backend.repository.PlayingTurnRepository;
import com.braintrain.backend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class PlayingTurnService {
    private final PlayingTurnRepository playingTurnRepository;
    private final ObjectiveRepository objectiveRepository;
    private final UserRepository userRepository;

    private final AuthService authService;
    private final ApiService apiService;
    private final ObjectiveService objectiveService;
    private final DailyWorkoutService dailyWorkoutService;

    private final PlayingTurnMapper playingTurnMapper;

    public ResponseEntity<Api<Void>> save(PlayingTurnDto playingTurnDto) {
        try {
            playingTurnRepository.save(playingTurnMapper.map(playingTurnDto));

            // update user's objectives
            updateUserObjectives(playingTurnDto.getGameType());

            // update user's daily workout tracking
            dailyWorkoutService.completeOneDailyWorkout(playingTurnDto.getGameType());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(apiService.generateSuccessApi(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiService.generateFailApi(
                            "internal_server_error",
                            "Error: " + e.getMessage()));
        }
    }

    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDates(
            GameType gameType,
            long fromUETime,
            long toUETime
    ) {
        User user = authService.getCurrentUser();

        return getPlayingTurnsByGameType(gameType, fromUETime, toUETime, user);
    }

    public ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsBetweenDatesForEachGame(
            GameName gameName,
            int level,
            long fromUETime,
            long toUETime
    ) {
        User user = authService.getCurrentUser();

        return getPlayingTurnsByGameName(gameName, level, fromUETime, toUETime, user);
    }

    public ResponseEntity<Api<PlayingTurnDto>> getBestTurnByEachGame(GameName gameName) {
        User user = authService.getCurrentUser();
        PlayingTurn turn = playingTurnRepository
                .findFirstByUserAndGameNameOrderByLevelDescCreatedDateDesc(user, gameName);
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(playingTurnMapper.mapToDto(turn)));
    }

    /*
    ** for admin: get playing turns by game type and created date
     */
    public ResponseEntity<Api<List<PlayingTurnDto>>> adminGetPlayingTurnsBetweenDates(
            UUID userId,
            GameType gameType,
            long fromUETime,
            long toUETime
    ) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "user_not_found",
                            "userId " + userId + " không tồn tại trong cơ sở dữ liệu",
                            null
                    ));
        }
        User user = userOptional.get();

        return getPlayingTurnsByGameType(gameType, fromUETime, toUETime, user);
    }

    /*
     ** for admin: get playing turns by game name and created date
     */
    public ResponseEntity<Api<List<PlayingTurnDto>>> adminGetPlayingTurnsBetweenDatesForEachGame(
            UUID userId,
            GameName gameName,
            int level,
            long fromUETime,
            long toUETime
    ) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(apiService.generateApi(
                            "user_not_found",
                            "userId " + userId + " không tồn tại trong cơ sở dữ liệu",
                            null
                    ));
        }
        User user = userOptional.get();

        return getPlayingTurnsByGameName(gameName, level, fromUETime, toUETime, user);
    }

    void delete(User user) {
        playingTurnRepository.deleteAllByUser(user);
    }

    private void updateUserObjectives(GameType gameType) {
        User user = authService.getCurrentUser();
        ObjectiveDataReader.ObjectiveData objectiveData = ObjectiveDataReader.getObjectiveData();

        /*
         ** TRAINING objectives
         */
        int turnsOfMonth = countPlayingTurnsByUserAndByMonth(user);

        List<ObjectiveDataReader.TrainingObj> allTrainingData = objectiveData.getTrainingData();
        for (ObjectiveDataReader.TrainingObj data : allTrainingData) {
            int level = data.getLevel();
            int noOfTurns = data.getNoOfTurns();

            Objective objective;
            if (turnsOfMonth == noOfTurns) {
                Optional<Objective> objectiveOptional = objectiveRepository
                        .findByUserAndObjectiveTypeAndLevel(user, ObjectiveType.TRAINING, level);
                objective = objectiveOptional.orElseGet(() -> objectiveService
                        .createTrainingObjective(level, noOfTurns, user));

                objective.setAchieved(true);
                objective.setAchievedDate(LocalDateTime.now());
                objectiveRepository.save(objective);

                break;
            }
        }

        /*
         ** CONSECUTIVE_TRAINING objectives
         */
        int conDaysInMonth = getMaxConsecutiveDays(user);

        List<ObjectiveDataReader.ConsecutiveTrainingObj> allConTrainingData = objectiveData.getConTrainingData();
        for (ObjectiveDataReader.ConsecutiveTrainingObj data : allConTrainingData) {
            int level = data.getLevel();
            int noOfDays = data.getNoOfDays();

            Objective objective;
            if (conDaysInMonth == noOfDays) {
                Optional<Objective> objectiveOptional = objectiveRepository
                        .findByUserAndObjectiveTypeAndLevel(user, ObjectiveType.CONSECUTIVE_TRAINING, level);
                objective = objectiveOptional.orElseGet(() -> objectiveService
                        .createConsecutiveTrainingObjective(level, noOfDays, user));

                objective.setAchieved(true);
                objective.setAchievedDate(LocalDateTime.now());
                objectiveRepository.save(objective);

                break;
            }
        }

        /*
         ** TRAINING_TIME objectives
         */
        double playingTimeInMonth = getTotalPlayTimeInMonth(user);

        List<ObjectiveDataReader.TrainingTimeObj> allTrainingTimeData = objectiveData.getTrainingTimeData();
        for (ObjectiveDataReader.TrainingTimeObj data : allTrainingTimeData) {
            int level = data.getLevel();
            double playingTime = data.getPlayingTime();

            Objective objective;
            if (playingTimeInMonth >= playingTime) {
                Optional<Objective> objectiveOptional = objectiveRepository
                        .findByUserAndObjectiveTypeAndLevel(user, ObjectiveType.TRAINING_TIME, level);
                objective = objectiveOptional.orElseGet(() -> objectiveService
                        .createTrainingTimeObjective(level, playingTime, user));

                objective.setAchieved(true);
                objective.setAchievedDate(LocalDateTime.now());
                objectiveRepository.save(objective);

                break;
            }
        }

        /*
         ** GAME_TYPE objectives
         */
        int timesInMonth = getNoOfTimesByGameType(user, gameType);
        ObjectiveType objectiveType = switch (gameType) {
            case MEMORY -> ObjectiveType.MEMORY_TRAINING;
            case ATTENTION -> ObjectiveType.ATTENTION_TRAINING;
            case LANGUAGE -> ObjectiveType.LANGUAGE_TRAINING;
            case MATH -> ObjectiveType.MATH_TRAINING;
        };

        List<ObjectiveDataReader.GameTypeTrainingObj> allGameTypeTrainingData = objectiveData.getGameTypeTrainingData();
        for (ObjectiveDataReader.GameTypeTrainingObj data : allGameTypeTrainingData) {
            int level = data.getLevel();
            int times = data.getTimes();

            Objective objective;
            if (timesInMonth == times) {
                Optional<Objective> objectiveOptional = objectiveRepository.findByUserAndObjectiveTypeAndLevel(
                        user,
                        objectiveType,
                        level
                );
                objective = objectiveOptional.orElseGet(() -> objectiveService
                        .createGameTypeObjective(level, times, gameType, user));

                objective.setAchieved(true);
                objective.setAchievedDate(LocalDateTime.now());
                objectiveRepository.save(objective);

                break;
            }
        }
    }

    private int countPlayingTurnsByUserAndByMonth(User user) {
        LocalDateTime firstDateOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<PlayingTurn> playingTurns = playingTurnRepository.findByUserAndCreatedDateAfterOrderByCreatedDateAsc(
                user,
                firstDateOfMonth
        );
        if (playingTurns.isEmpty()) {
            return 0;
        }
        return playingTurns.size();
    }

    private int getMaxConsecutiveDays(User user) {
        int consecutiveDays = 1;
        int bestRecord = 0;

        LocalDateTime firstDateOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<PlayingTurn> playingTurns = playingTurnRepository.findByUserAndCreatedDateAfterOrderByCreatedDateAsc(
                user,
                firstDateOfMonth
        );
        if (playingTurns.isEmpty()) {
            return 0;
        }

        List<Integer> playDateInMonth = playingTurns
                .stream()
                .map((turn) -> turn.getCreatedDate().getDayOfMonth())
                .toList();
        if (playDateInMonth.size() < 2) {
            return 1;
        } else {
            for (int i = 0; i < playDateInMonth.size() - 1; i++) {
                if (playDateInMonth.get(i + 1) == playDateInMonth.get(i) + 1) {
                    consecutiveDays++;
                    if (bestRecord < consecutiveDays) {
                        bestRecord = consecutiveDays;
                    }
                } else {
                    consecutiveDays = 0;
                }
            }
        }

        return bestRecord;
    }

    private double getTotalPlayTimeInMonth(User user) {
        LocalDateTime firstDateOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<PlayingTurn> playingTurns = playingTurnRepository.findByUserAndCreatedDateAfterOrderByCreatedDateAsc(
                user,
                firstDateOfMonth
        );
        if (playingTurns.isEmpty()) {
            return 0;
        }
        return playingTurns.stream()
                .mapToDouble(turn -> (double) turn.getPlayTime() / 3600)
                .sum();
    }

    private int getNoOfTimesByGameType(User user, GameType gameType) {
        LocalDateTime firstDateOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        List<PlayingTurn> playingTurns = playingTurnRepository.findByUserAndGameTypeAndCreatedDateAfter(
                user,
                gameType,
                firstDateOfMonth
        );
        if (playingTurns.isEmpty()) {
            return 0;
        }
        return playingTurns.size();
    }

    private ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsByGameType(
            GameType gameType,
            long fromUETime,
            long toUETime,
            User user
    ) {
        List<PlayingTurn> turnOptional = playingTurnRepository
                .findByUserAndGameTypeAndCreatedDateBetween(
                        user,
                        gameType,
                        convertToDateTime(fromUETime),
                        convertToDateTime(toUETime),
                        Sort.by(Sort.Direction.ASC, "createdDate")
                );
        List<PlayingTurnDto> list = turnOptional.stream()
                .map(playingTurnMapper::mapToDto)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(list));
    }

    private ResponseEntity<Api<List<PlayingTurnDto>>> getPlayingTurnsByGameName(
            GameName gameName,
            int level,
            long fromUETime,
            long toUETime,
            User user
    ) {
        List<PlayingTurn> turnOptional = playingTurnRepository
                .findByUserAndGameNameAndLevelAndCreatedDateBetweenOrderByCreatedDateAsc(
                        user,
                        gameName,
                        level,
                        convertToDateTime(fromUETime),
                        convertToDateTime(toUETime)
                );
        List<PlayingTurnDto> list = turnOptional.stream()
                .map(playingTurnMapper::mapToDto)
                .toList();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(list));
    }

    private LocalDateTime convertToDateTime(long UETime) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(UETime), ZoneId.systemDefault());
    }
}
