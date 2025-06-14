package org.example.project1.service.tariffservice;


import org.example.project1.entity.Gym;
import org.example.project1.entity.Tariff;
import org.example.project1.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface TariffService {
    ResponseEntity<?> findAll();
    Tariff save(Tariff tariff);
    Tariff saveTariffWithUserAndGym(Tariff tariff, User user, Gym gym);
    ResponseEntity<?> delete(UUID id);
ResponseEntity<?> update(UUID id, Tariff tariff);


}
