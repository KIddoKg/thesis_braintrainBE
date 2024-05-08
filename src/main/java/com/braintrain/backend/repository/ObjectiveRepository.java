package com.braintrain.backend.repository;

import com.braintrain.backend.entity.Objective;
import com.braintrain.backend.entity.ObjectiveType;
import com.braintrain.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {
    Optional<Objective> findByUserAndObjectiveTypeAndLevel(
            User user,
            ObjectiveType objectiveType,
            int level);

    List<Objective> findByUserAndIsAchievedOrderByObjectiveTypeAscLevelAsc(User user, boolean isAchieved);

    List<Objective> findByUser(User user);

    void deleteAllByUser(User user);
}
