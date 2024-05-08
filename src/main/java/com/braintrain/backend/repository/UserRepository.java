package com.braintrain.backend.repository;

import com.braintrain.backend.entity.User;
import com.braintrain.backend.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    Page<User> findByUserRole(UserRole userRole, PageRequest pageRequest);

    Page<User> findByIsMonitored(boolean isMonitored, PageRequest pageRequest);

    @Query(
            value = "SELECT u FROM User u WHERE u.isLocked = true"
    )
    Page<User> findByIsLocked(PageRequest pageRequest);
}
