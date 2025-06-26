package org.example.project1.service.subscriptionType;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.SubscriptionType;
import org.example.project1.entity.Gym;
import org.example.project1.repository.SubscriptionTypeRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionTypeServiceImpl implements SubscriptionTypeService {
    private final SubscriptionTypeRepository subscriptionTypeRepository;

    public List<SubscriptionType> findAll() {
        return subscriptionTypeRepository.findAll();
    }

    public SubscriptionType save(SubscriptionType subscriptionType) {
        return subscriptionTypeRepository.save(subscriptionType);
    }

    public void delete(UUID id) {
        subscriptionTypeRepository.deleteById(id);
    }

    public SubscriptionType update(UUID id, SubscriptionType subscriptionType) {
        SubscriptionType existing = subscriptionTypeRepository.findById(id).orElseThrow();
        existing.setName(subscriptionType.getName());
        existing.setPrice(subscriptionType.getPrice());
        existing.setDuration(subscriptionType.getDuration());
        existing.setDescription(subscriptionType.getDescription());
        existing.setType(subscriptionType.getType());
        existing.setGym(subscriptionType.getGym());
        return subscriptionTypeRepository.save(existing);
    }

    public List<SubscriptionType> findByGym(Gym gym) {
        return subscriptionTypeRepository.findByGym(gym);
    }
}
