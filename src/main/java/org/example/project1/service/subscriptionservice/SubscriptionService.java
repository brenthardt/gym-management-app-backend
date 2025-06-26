package org.example.project1.service.subscriptionservice;

import org.example.project1.entity.Subscription;
import org.example.project1.entity.SubscriptionType;
import org.example.project1.entity.User;


import java.util.List;

public interface SubscriptionService {
    Subscription subscribeUserToSubscriptionType(User user, SubscriptionType subscriptionType, Integer price, Integer duration);
    List<Subscription> getActiveSubscriptions(User user);
    void deactivateSubscription(Subscription subscription);
    Subscription getActiveSubscription(User user, SubscriptionType subscriptionType);
}