package org.example.project1.service.subscriptionType;

import org.example.project1.entity.SubscriptionType;
import org.example.project1.entity.Gym;
import java.util.List;
import java.util.UUID;

public interface SubscriptionTypeService {
    List<SubscriptionType> findAll();
    SubscriptionType save(SubscriptionType subscriptionType);
    void delete(UUID id);
    SubscriptionType update(UUID id, SubscriptionType subscriptionType);
    List<SubscriptionType> findByGym(Gym gym);
}
