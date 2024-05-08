package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.dto.UserRanking;
import com.braintrain.backend.dto.RankingDto;
import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.entity.PlayingTurn;
import com.braintrain.backend.entity.User;
import com.braintrain.backend.repository.PlayingTurnRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@AllArgsConstructor
public class RankingService {
    private final PlayingTurnRepository playingTurnRepository;
    private final ApiService apiService;
    private final AuthService authService;

    public ResponseEntity<Api<RankingDto>> getRankingByGameName(GameName gameName, char rankBy) {
        // if rankBy = 'w' -> start from first date of week
        // if rankBy = 'm' -> start from first date of month
        Calendar calendar = Calendar.getInstance();
        int maxSize = 20;
        if (rankBy == 'w') {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            // if today is Sunday, move back to the previous week's Monday
            if (calendar.getTime().after(new Date())) {
                calendar.add(Calendar.DAY_OF_WEEK, -7);
            }
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            maxSize = 30;
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        LocalDateTime startDate = calendar.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // get and desc sort all playing turns of all users in the current week
        List<PlayingTurn> turnList = playingTurnRepository
                .findByGameNameAndCreatedDateAfterOrderByScoreDesc(gameName, startDate);

        // get the highest result of each user
        List<UserRanking> highestRanks = getRanksOfAllUsers(turnList);

        // 20 highest user ranks
        List<UserRanking> listOfHighestUsers = highestRanks.stream().limit(maxSize).toList();

        // get rank of current user
        int userRank = getUserRank(highestRanks);

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(
                        RankingDto.builder()
                                .myRank(userRank)
                                .userRankings(listOfHighestUsers)
                                .build()));
    }

    public ResponseEntity<Api<RankingDto>> getAllRankingByGameName(GameName gameName) {
        List<PlayingTurn> turnList = playingTurnRepository.findByGameNameOrderByScoreDesc(gameName);
        List<UserRanking> highestRanks = getRanksOfAllUsers(turnList);
        List<UserRanking> listOfHighestUsers = highestRanks.stream().limit(50).toList();

        int userRank = getUserRank((highestRanks));

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiService.generateSuccessApi(
                        RankingDto.builder()
                                .myRank(userRank)
                                .userRankings(listOfHighestUsers)
                                .build()));
    }

    private int getUserRank(List<UserRanking> highestRanks) {
        User user = authService.getCurrentUser();

        if (highestRanks.isEmpty()) {
            return 0;
        }

        int userRank = 1;
        // find user rank
        for (UserRanking rank : highestRanks) {
            if (rank.getUserId() == user.getId()) {
                break;
            }
            userRank++;
        }
        return userRank;
    }

    private List<UserRanking> getRanksOfAllUsers(List<PlayingTurn> turnList) {
        // for each user, only get the playing turn with the highest score
        List<PlayingTurn> distinctList = new ArrayList<>();
        Set<UUID> userIdSet = new HashSet<>();

        for (PlayingTurn turn : turnList) {
            UUID userId = turn.getUser().getId();
            if (!userIdSet.contains(userId)) {
                userIdSet.add(userId);
                distinctList.add(turn);
            }
        }

        return distinctList.stream()
                .map(this::mapToRankingDto)
                .toList();
    }

    private UserRanking mapToRankingDto(PlayingTurn playingTurn) {
        return UserRanking.builder()
                .userId(playingTurn.getUser().getId())
                .userName(playingTurn.getUser().getFullName())
                .score(playingTurn.getScore())
                .build();
    }
}
