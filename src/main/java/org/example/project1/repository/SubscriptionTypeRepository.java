package org.example.project1.repository;

import org.example.project1.entity.SubscriptionType;
import org.example.project1.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionTypeRepository extends JpaRepository<SubscriptionType, UUID> {
    Optional<SubscriptionType> findByName(String name);
    List<SubscriptionType> findByGym(Gym gym);
}
