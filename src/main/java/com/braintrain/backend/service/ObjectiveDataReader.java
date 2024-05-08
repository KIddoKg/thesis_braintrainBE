package com.braintrain.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
public class ObjectiveDataReader {
    private static ObjectiveData objectiveData;

    public static ObjectiveData getObjectiveData() {
        if (objectiveData == null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                InputStream inputStream = ObjectiveDataReader.class
                        .getResourceAsStream("/data/ObjectiveData.json");
                objectiveData = objectMapper.readValue(inputStream, new TypeReference<>(){});
            } catch (IOException e) {
                log.error("Error while reading json file: " + e.getMessage());
            }
        }
        return objectiveData;
    }

    @Getter
    public static class ObjectiveData {
        private List<TrainingObj> trainingData;
        private List<ConsecutiveTrainingObj> conTrainingData;
        private List<TrainingTimeObj> trainingTimeData;
        private List<GameTypeTrainingObj> gameTypeTrainingData;
    }

    @Getter
    public static class TrainingObj {
        private int level;
        private int noOfTurns;
    }

    @Getter
    public static class ConsecutiveTrainingObj {
        private int level;
        private int noOfDays;
    }

    @Getter
    public static class TrainingTimeObj {
        private int level;
        private double playingTime;
    }

    @Getter
    public static class GameTypeTrainingObj {
        private int level;
        private int times;
    }
}
