package org.example.project1.repository;

import org.example.project1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByTelegramChatId(Long telegramChatId);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.telegramChatId = :telegramChatId")
    Optional<User> findByTelegramChatIdWithRoles(@Param("telegramChatId") Long telegramChatId);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    Optional<User> findByName(String name);
}
