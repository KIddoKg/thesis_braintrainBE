package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.ObjectiveDto;
import com.braintrain.backend.entity.*;
import com.braintrain.backend.repository.ObjectiveRepository;
import com.braintrain.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ObjectiveService {
    private final ObjectiveRepository objectiveRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final ApiService apiService;

    public ObjectiveService(ObjectiveRepository objectiveRepository,
                            UserRepository userRepository,
                            @Lazy AuthService authService,
                            ApiService apiService) {
        this.objectiveRepository = objectiveRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.apiService = apiService;
    }

    public void createAllObjectives(User user) {
        try {
            ObjectiveDataReader.ObjectiveData objectiveData = ObjectiveDataReader.getObjectiveData();

            // create TRAINING objectives
            List<ObjectiveDataReader.TrainingObj> allTrainingData = objectiveData.getTrainingData();
            for (ObjectiveDataReader.TrainingObj data : allTrainingData) {
                int level = data.getLevel();
                int noOfTurns = data.getNoOfTurns();
                createTrainingObjective(level, noOfTurns, user);
            }

            // create CONSECUTIVE_TRAINING objectives
            List<ObjectiveDataReader.ConsecutiveTrainingObj> allConTrainingData = objectiveData.getConTrainingData();
            for (ObjectiveDataReader.ConsecutiveTrainingObj data : allConTrainingData) {
                int level = data.getLevel();
                int noOfDays = data.getNoOfDays();
                createConsecutiveTrainingObjective(level, noOfDays, user);
            }

            // create TRAINING_TIME objectives
            List<ObjectiveDataReader.TrainingTimeObj> allTrainingTimeData = objectiveData.getTrainingTimeData();
            for (ObjectiveDataReader.TrainingTimeObj data : allTrainingTimeData) {
                int level = data.getLevel();
                double playingTime = data.getPlayingTime();
                createTrainingTimeObjective(level, playingTime, user);
            }

            // create GAME_TYPE objectives
            List<ObjectiveDataReader.GameTypeTrainingObj> allGameTypeTrainingData = objectiveData
                    .getGameTypeTrainingData();
            GameType[] gameTypeList = GameType.values();
            for (GameType gameType : gameTypeList) {
                for (ObjectiveDataReader.GameTypeTrainingObj data : allGameTypeTrainingData) {
                    int level = data.getLevel();
                    int times = data.getTimes();
                    createGameTypeObjective(level, times, gameType, user);
                }
            }
        } catch (Exception e) {
            log.error("Error while creating all objectives: " + e.getMessage());
        }
    }

    public ResponseEntity<Api<List<ObjectiveDto>>> getAllObjectivesOfUser() {
        User user = authService.getCurrentUser();

        List<Objective> achievedList = objectiveRepository
                .findByUserAndIsAchievedOrderByObjectiveTypeAscLevelAsc(user, true);
        List<Objective> unachievedList = objectiveRepository
                .findByUserAndIsAchievedOrderByObjectiveTypeAscLevelAsc(user, false);

        List<Objective> sortedList = new ArrayList<>();
        sortedList.addAll(sortObjectives(achievedList));
        sortedList.addAll(sortObjectives(unachievedList));

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(sortedList.stream().map(this::mapToDto).toList()));
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void resetObjectives() {
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            List<Objective> allObjectives = objectiveRepository.findByUser(user);
            for (Objective objective : allObjectives) {
                objective.setAchieved(false);
                objective.setAchievedDate(null);
            }
            objectiveRepository.saveAll(allObjectives);
        }
    }

    public Objective createTrainingObjective(int level, int noOfTurns, User user) {
        Objective obj = Objective.builder()
                .objectiveType(ObjectiveType.TRAINING)
                .level(level)
                .quantityRequired(noOfTurns)
                .title("Hoàn thành " + noOfTurns + " bài tập")
                .description("Bạn đã hoàn thành " + noOfTurns + " trò chơi bất kì")
                .isAchieved(false)
                .achievedDate(null)
                .createdDate(LocalDateTime.now())
                .user(user)
                .build();
        return objectiveRepository.save(obj);
    }

    public Objective createConsecutiveTrainingObjective(int level, int noOfDays, User user) {
        Objective obj = Objective.builder()
                .objectiveType(ObjectiveType.CONSECUTIVE_TRAINING)
                .level(level)
                .quantityRequired(noOfDays)
                .title("Rèn luyện " + noOfDays + " ngày liên tục")
                .description("Bạn đã rèn luyện trong " + noOfDays + " ngày liên tục")
                .isAchieved(false)
                .achievedDate(null)
                .createdDate(LocalDateTime.now())
                .user(user)
                .build();
        return objectiveRepository.save(obj);
    }

    public Objective createTrainingTimeObjective(int level, double hrs, User user) {
        Objective obj = Objective.builder()
                .objectiveType(ObjectiveType.TRAINING_TIME)
                .level(level)
                .quantityRequired(hrs)
                .title(hrs + " tiếng rèn luyện")
                .description("Bạn đã dành ra " + hrs + " tiếng rèn luyện nhận thức")
                .isAchieved(false)
                .achievedDate(null)
                .createdDate(LocalDateTime.now())
                .user(user)
                .build();
        return objectiveRepository.save(obj);
    }

    public Objective createGameTypeObjective(int level, int times, GameType gameType, User user) {
        String gameName = "";
        ObjectiveType objectiveType = null;

        switch (gameType) {
            case MEMORY -> {
                gameName = "Trí nhớ";
                objectiveType = ObjectiveType.MEMORY_TRAINING;
            }
            case ATTENTION -> {
                gameName = "Tập trung";
                objectiveType = ObjectiveType.ATTENTION_TRAINING;
            }
            case LANGUAGE -> {
                gameName = "Ngôn ngữ";
                objectiveType = ObjectiveType.LANGUAGE_TRAINING;
            }
            case MATH -> {
                gameName = "Toán";
                objectiveType = ObjectiveType.MATH_TRAINING;
            }
        }

        Objective obj = Objective.builder()
                .objectiveType(objectiveType)
                .level(level)
                .quantityRequired(times)
                .title("Chơi " + gameName + " " + times + " lần")
                .description("Bạn đã hoàn thành " + times + " lượt chơi trò " + gameName)
                .isAchieved(false)
                .achievedDate(null)
                .createdDate(LocalDateTime.now())
                .user(user)
                .build();
        return objectiveRepository.save(obj);
    }

    void delete(User user) {
        objectiveRepository.deleteAllByUser(user);
    }

    private ObjectiveDto mapToDto(Objective objective) {
        return ObjectiveDto.builder()
                .id(objective.getId())
                .objectiveType(objective.getObjectiveType())
                .level(objective.getLevel())
                .quantityRequired(objective.getQuantityRequired())
                .title(objective.getTitle())
                .description(objective.getDescription())
                .isAchieved(objective.isAchieved())
                .achievedDate(convertToEpochMillis(objective.getAchievedDate()))
                .build();
    }

    private Long convertToEpochMillis(LocalDateTime achievedDate) {
        if (achievedDate != null) {
            return achievedDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return null;
    }

    private List<Objective> sortObjectives(List<Objective> list) {
        List<Objective> sortedList = new ArrayList<>();
        boolean start = false;

        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.TRAINING) {
                sortedList.add(objective);
                start = true;
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.CONSECUTIVE_TRAINING) {
                sortedList.add(objective);
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.TRAINING_TIME) {
                sortedList.add(objective);
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.MEMORY_TRAINING) {
                sortedList.add(objective);
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.ATTENTION_TRAINING) {
                sortedList.add(objective);
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.LANGUAGE_TRAINING) {
                sortedList.add(objective);
            } else {
                if (start) {
                    start = false;
                    break;
                }
            }
        }
        for (Objective objective : list) {
            if (objective.getObjectiveType() == ObjectiveType.MATH_TRAINING) {
                sortedList.add(objective);
            } else {
                if (start) {
                    break;
                }
            }
        }

        return sortedList;
    }
}
