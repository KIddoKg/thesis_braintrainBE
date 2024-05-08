package com.braintrain.backend.repository;

import com.braintrain.backend.entity.Otp;
import com.braintrain.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findByOtp(String otp);

    Boolean existsByOtp(String otp);

    @Query(
            value = "select * from otp where user_id = ?1 and confirmed_at is null",
            nativeQuery = true
    )
    Optional<Otp> findUnconfirmedOtpByUser(UUID userId);

    void deleteAllByUser(User user);
}
