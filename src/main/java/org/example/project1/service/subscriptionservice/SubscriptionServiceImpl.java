package org.example.project1.service.subscriptionservice;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.Subscription;
import org.example.project1.entity.SubscriptionType;
import org.example.project1.entity.User;
import org.example.project1.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public Subscription subscribeUserToSubscriptionType(User user, SubscriptionType subscriptionType, Integer price, Integer duration) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setSubscriptionType(subscriptionType);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(duration));
        subscription.setPrice(price != null ? price.doubleValue() : null);
        subscription.setStatus(true);
        subscription.setDuration(duration);
        subscription.setPurchaseDate(LocalDate.now());
        return subscriptionRepository.save(subscription);
    }

    @Override
    public List<Subscription> getActiveSubscriptions(User user) {
        return subscriptionRepository.findByUserAndStatusTrue(user);
    }

    @Override
    public void deactivateSubscription(Subscription subscription) {
        subscription.setStatus(false);
        subscriptionRepository.save(subscription);
    }

    @Override
    public Subscription getActiveSubscription(User user, SubscriptionType subscriptionType) {
        return getActiveSubscriptions(user).stream()
            .filter(s -> s.getSubscriptionType().equals(subscriptionType))
            .findFirst().orElse(null);
    }
} 
