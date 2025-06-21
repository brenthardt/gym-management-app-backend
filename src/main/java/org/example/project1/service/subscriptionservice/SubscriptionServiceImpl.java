package org.example.project1.service.subscriptionservice;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.Subscription;
import org.example.project1.entity.User;
import org.example.project1.entity.Tariff;
import org.example.project1.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public Subscription subscribeUserToTariff(User user, Tariff tariff, Integer price, Integer duration) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setTariff(tariff);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(duration));
        subscription.setPrice(price);
        subscription.setStatus(true);
        subscription.setDuration(duration);
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
    public Subscription getActiveSubscription(User user, Tariff tariff) {
        return subscriptionRepository.findByUserAndTariffAndStatusTrue(user, tariff).orElse(null);
    }
} 
