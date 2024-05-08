package com.braintrain.backend.repository;

import com.braintrain.backend.entity.GameName;
import com.braintrain.backend.entity.GameType;
import com.braintrain.backend.entity.PlayingTurn;
import com.braintrain.backend.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PlayingTurnRepository extends JpaRepository<PlayingTurn, UUID> {
    // for creating objectives
    List<PlayingTurn> findByUserAndCreatedDateAfterOrderByCreatedDateAsc(User user, LocalDateTime createdDate);

    // for creating objective
    List<PlayingTurn> findByUserAndGameTypeAndCreatedDateAfter(
            User user,
            GameType gameType,
            LocalDateTime createdDate);

    // for charts and for admin: get word list of user
    List<PlayingTurn> findByUserAndGameTypeAndCreatedDateBetween(
            User user,
            GameType gameType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Sort sort);

    // for charts
    List<PlayingTurn> findByUserAndGameNameAndLevelAndCreatedDateBetweenOrderByCreatedDateAsc(
            User user,
            GameName gameName,
            int level,
            LocalDateTime fromDate,
            LocalDateTime toDate);

    // for getting the highest level of user
    PlayingTurn findFirstByUserAndGameNameOrderByLevelDescCreatedDateDesc(User user, GameName gameName);

    // for ranking users
    List<PlayingTurn> findByGameNameAndCreatedDateAfterOrderByScoreDesc(
            GameName gameName,
            LocalDateTime createdDate);

    // for ranking (from the start)
    List<PlayingTurn> findByGameNameOrderByScoreDesc(GameName gameName);

    // for admin: getting ghost users
    @Query(
            nativeQuery = true,
            value = "select P.user_id, U.full_name, U.phone, max(P.created_date) " +
                    "from playing_turn P, user U where P.user_id = U.id group by P.user_id"
    )
    List<Object[]> findTheLatestPlayingTurnOfAllUsers();

    void deleteAllByUser(User user);
}
