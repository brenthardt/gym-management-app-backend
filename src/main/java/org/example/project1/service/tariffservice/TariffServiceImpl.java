package org.example.project1.impl;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.Tariff;
import org.example.project1.repository.TariffRepository;
import org.example.project1.service.tariffservice.TariffService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;

    @Override
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(tariffRepository.findAll());
    }

    @Override
    public Tariff save(Tariff tariff) {
        return tariffRepository.save(tariff);
    }

    @Override
    public ResponseEntity<?> delete(UUID id) {
        if (tariffRepository.existsById(id)) {
            tariffRepository.deleteById(id);
            return ResponseEntity.ok("Tariff deleted");
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<?> update(UUID id, Tariff tariff) {
        return tariffRepository.findById(id)
                .map(existingTariff -> {
                    existingTariff.setName(tariff.getName());
                    existingTariff.setPrice(tariff.getPrice());
                    existingTariff.setEndDate(tariff.getEndDate());
                    existingTariff.setDuration(tariff.getDuration());
                    existingTariff.setPlanType(tariff.getPlanType());
                    existingTariff.setGym(tariff.getGym());


                    return ResponseEntity.ok(tariffRepository.save(existingTariff));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
