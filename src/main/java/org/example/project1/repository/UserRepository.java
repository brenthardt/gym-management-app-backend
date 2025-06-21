package org.example.project1.repository;

import org.example.project1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.example.project1.entity.BotStep;
import org.example.project1.entity.Gym;

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

    @Query("SELECT u FROM User u WHERE u.step = :step AND u.telegramChatId = :telegramChatId")
    Optional<User> findByStepAndTelegramChatId(@Param("step") BotStep step, @Param("telegramChatId") Long telegramChatId);

    @Query("SELECT DISTINCT g FROM User u JOIN u.gyms g LEFT JOIN FETCH g.members WHERE u.id = :userId")
    List<Gym> findAdminGymsWithMembers(@Param("userId") UUID userId);

    @Query("SELECT phone, COUNT(*) as count FROM User GROUP BY phone HAVING COUNT(*) > 1")
    List<Object[]> findDuplicatePhones();

    @Query("SELECT u FROM User u WHERE u.phone = :phone ORDER BY u.id")
    List<User> findAllByPhone(@Param("phone") String phone);

    @Query("SELECT u FROM User u WHERE u.phone = :phone ORDER BY u.id")
    Optional<User> findFirstByPhone(@Param("phone") String phone);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.phone = u.phone GROUP BY u2.phone)")
    void deleteDuplicateUsers();
    
    @Query("UPDATE User u SET u.botBlocked = :blocked WHERE u.telegramChatId = :chatId")
    void updateBotBlockedStatus(@Param("chatId") Long chatId, @Param("blocked") Boolean blocked);
    
    @Query("SELECT u FROM User u WHERE u.telegramChatId = :telegramChatId ORDER BY u.id")
    List<User> findAllByTelegramChatId(@Param("telegramChatId") Long telegramChatId);
    
    @Query("SELECT telegramChatId, COUNT(*) as count FROM User WHERE telegramChatId IS NOT NULL GROUP BY telegramChatId HAVING COUNT(*) > 1")
    List<Object[]> findDuplicateTelegramChatIds();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.telegramChatId = u.telegramChatId AND u2.telegramChatId IS NOT NULL GROUP BY u2.telegramChatId)")
    void deleteDuplicateTelegramChatIdUsers();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.user.id IN (SELECT u.id FROM User u WHERE u.telegramChatId = :telegramChatId AND u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.telegramChatId = :telegramChatId AND u2.telegramChatId IS NOT NULL GROUP BY u2.telegramChatId))")
    void deleteSubscriptionsForDuplicateUsers(@Param("telegramChatId") Long telegramChatId);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.subscription = NULL WHERE u.telegramChatId = :telegramChatId AND u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.telegramChatId = :telegramChatId AND u2.telegramChatId IS NOT NULL GROUP BY u2.telegramChatId)")
    void clearSubscriptionForDuplicateUsers(@Param("telegramChatId") Long telegramChatId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.user.id IN (SELECT u.id FROM User u WHERE u.telegramChatId IN (SELECT telegramChatId FROM User WHERE telegramChatId IS NOT NULL GROUP BY telegramChatId HAVING COUNT(*) > 1) AND u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.telegramChatId = u.telegramChatId AND u2.telegramChatId IS NOT NULL GROUP BY u2.telegramChatId))")
    void deleteAllSubscriptionsForDuplicateUsers();
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Subscription s WHERE s.user.id IN (SELECT u.id FROM User u WHERE u.phone IN (SELECT phone FROM User GROUP BY phone HAVING COUNT(*) > 1) AND u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.phone = u.phone GROUP BY u2.phone))")
    void deleteAllSubscriptionsForDuplicatePhoneUsers();
}
