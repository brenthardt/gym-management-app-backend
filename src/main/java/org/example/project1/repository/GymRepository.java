package org.example.project1.repository;

import org.example.project1.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GymRepository extends JpaRepository<Gym, UUID> {
    Optional<Gym> findByName(String name);

    @Query("SELECT DISTINCT g FROM Gym g LEFT JOIN FETCH g.subscriptionTypes")
    List<Gym> findAllWithSubscriptionTypes();

    @Query("SELECT g FROM Gym g WHERE LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(g.location) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Gym> searchByNameOrLocation(String query);

    @Query("SELECT DISTINCT g FROM Gym g LEFT JOIN FETCH g.members WHERE g.id IN :ids")
    List<Gym> findAllByIdInWithMembers(List<UUID> ids);
}
