package org.example.project1.service.subscriptionservice;

import org.example.project1.entity.Subscription;
import org.example.project1.entity.User;
import org.example.project1.entity.Tariff;

import java.util.List;

public interface SubscriptionService {
    Subscription subscribeUserToTariff(User user, Tariff tariff, Integer price, Integer duration);
    List<Subscription> getActiveSubscriptions(User user);
    void deactivateSubscription(Subscription subscription);
    Subscription getActiveSubscription(User user, Tariff tariff);
} 