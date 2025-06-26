package org.example.project1.repository;

import org.example.project1.entity.Subscription;
import org.example.project1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUser(User user);
    List<Subscription> findByUserAndStatusTrue(User user);
    void deleteAllBySubscriptionType(org.example.project1.entity.SubscriptionType subscriptionType);
} 