package com.braintrain.backend.repository;

import com.braintrain.backend.entity.DailyWorkout;
import com.braintrain.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyWorkoutRepository extends JpaRepository<DailyWorkout, UUID> {
    Optional<DailyWorkout> findByUserAndCreatedDate(User user, LocalDate createdDate);

    void deleteAllByUser(User user);
}
