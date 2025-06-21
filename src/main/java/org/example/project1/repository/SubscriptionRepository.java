package org.example.project1.repository;

import org.example.project1.entity.Subscription;
import org.example.project1.entity.User;
import org.example.project1.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUser(User user);
    List<Subscription> findByTariff(Tariff tariff);
    Optional<Subscription> findByUserAndTariffAndStatusTrue(User user, Tariff tariff);
    List<Subscription> findByUserAndStatusTrue(User user);
} 