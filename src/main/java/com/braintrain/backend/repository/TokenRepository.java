package com.braintrain.backend.repository;

import com.braintrain.backend.entity.Token;
import com.braintrain.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    Optional<Token> findByToken(String token);

    @Query(
            nativeQuery = true,
            value = "select * from token where user_id = ?1 and (expired = false or revoked = false)"
    )
    List<Token> findAllValidTokenByUser(UUID userId);

    void deleteAllByUser(User user);
}
