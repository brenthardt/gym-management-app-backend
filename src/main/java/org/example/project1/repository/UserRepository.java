package org.example.project1.repository;

import org.example.project1.entity.Gym;
import org.example.project1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    Optional<User> findByPhone(String phone);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.telegramChatId = :chatId")
    Optional<User> findByTelegramChatIdWithRoles(@Param("chatId") Long chatId);

    Optional<User> findByTelegramChatId(Long telegramChatId);

    List<User> findAllByTelegramChatId(Long telegramChatId);

    List<User> findAllByPhone(String phone);

    Optional<User> findFirstByPhone(String phone);

    @Modifying
    @Query(value = "DELETE FROM users WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER(PARTITION BY telegram_chat_id ORDER BY id) as rn FROM users WHERE telegram_chat_id IS NOT NULL) t WHERE t.rn > 1)", nativeQuery = true)
    void deleteDuplicateTelegramChatIdUsers();

    @Query("SELECT g FROM Gym g LEFT JOIN FETCH g.members m WHERE m.id = :userId")
    List<Gym> findGymsByMemberId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.user.id IN (SELECT u.id FROM User u WHERE u.telegramChatId = :chatId AND u.id NOT IN (SELECT MIN(u2.id) FROM User u2 WHERE u2.telegramChatId = :chatId))")
    void deleteSubscriptionsForDuplicateUsers(@Param("chatId") Long chatId);
    
    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.user.id IN (SELECT u.id FROM User u WHERE u.id NOT IN (SELECT MIN(u2.id) FROM User u2 GROUP BY u2.telegramChatId) AND u.telegramChatId IS NOT NULL)")
    void deleteAllSubscriptionsForDuplicateUsers();

    @Modifying
    @Query(value = "DELETE FROM users WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() OVER(PARTITION BY phone ORDER BY id) as rn FROM users) t WHERE t.rn > 1)", nativeQuery = true)
    void deleteDuplicateUsers();
    
    Optional<User> findByRefreshToken(String refreshToken);

    @Query("SELECT u.phone, COUNT(u) FROM User u GROUP BY u.phone HAVING COUNT(u) > 1")
    List<Object[]> findDuplicatePhones();

    List<User> findByPhoneStartingWith(String phonePrefix);
    List<User> findByNameContainingIgnoreCase(String name);
}
