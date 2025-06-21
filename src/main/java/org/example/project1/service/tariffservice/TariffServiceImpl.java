package org.example.project1.service.tariffservice;

import lombok.RequiredArgsConstructor;
import org.example.project1.dto.GymDto;
import org.example.project1.dto.TariffDto;
import org.example.project1.entity.Gym;
import org.example.project1.entity.Tariff;
import org.example.project1.entity.User;
import org.example.project1.repository.GymRepository;
import org.example.project1.repository.TariffRepository;
import org.example.project1.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;
    private final UserRepository userRepository;
    private final GymRepository gymRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void deleteExpiredTariffs() {
        Date now = new Date();
        List<Tariff> expiredTariffs = tariffRepository.findByEndDateBefore(now);

        for (Tariff tariff : expiredTariffs) {
            this.delete(tariff.getId());
        }
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanUpTariffsWithZeroDuration() {
        List<Tariff> zeroDurationTariffs = tariffRepository.findByDuration(0);

        for (Tariff tariff : zeroDurationTariffs) {
            Gym gym = tariff.getGym();
            List<User> users = new ArrayList<>(tariff.getUsers());

            for (User user : users) {
                user.getTariffs().remove(tariff);

                boolean hasOtherTariffForGym = user.getTariffs().stream()
                        .anyMatch(t -> t.getGym() != null && gym != null && t.getGym().getId().equals(gym.getId()));
                if (gym != null && !hasOtherTariffForGym) {
                    user.getGyms().remove(gym);
                    gym.getMembers().remove(user);
                }
            }
            userRepository.saveAll(users);

            if (gym != null) {
                gym.getTariffs().remove(tariff);
                gymRepository.save(gym);
                tariff.setGym(null);
            }

            tariff.getUsers().clear();

            tariffRepository.delete(tariff);
        }
    }

    @Override
    public ResponseEntity<?> findAll() {
        var tariffs = tariffRepository.findAll();
        var dtos = tariffs.stream().map(this::mapToDto).toList();
        return ResponseEntity.ok(dtos);
    }

    private TariffDto mapToDto(Tariff tariff) {
        TariffDto dto = new TariffDto();
        dto.setId(tariff.getId());
        dto.setName(tariff.getName());
        dto.setPrice(tariff.getPrice());
        dto.setEndDate(tariff.getEndDate());
        dto.setDuration(tariff.getDuration());

        if (tariff.getGym() != null) {
            GymDto gymDto = new GymDto();
            gymDto.setId(tariff.getGym().getId());
            gymDto.setName(tariff.getGym().getName());
            dto.setGym(gymDto);
        }

        return dto;
    }

    @Override
    public Tariff save(Tariff tariff) {
        if (tariff.getGym() != null && tariff.getGym().getId() != null) {
            Gym gym = gymRepository.findById(tariff.getGym().getId())
                    .orElseThrow(() -> new RuntimeException("Gym not found"));
            tariff.setGym(gym);

            if (gym.getTariffs() == null) {
                gym.setTariffs(new ArrayList<>());
            }
            if (gym.getTariffs().stream().noneMatch(t -> t.getId().equals(tariff.getId()))) {
                gym.getTariffs().add(tariff);
            }

            gymRepository.save(gym);
        }
        return tariffRepository.save(tariff);
    }

    @Transactional
    public Tariff saveTariffWithUserAndGym(Tariff tariff, User user, Gym gym) {
        tariff.setGym(gym);

        tariff.getUsers().add(user);
        user.getTariffs().add(tariff);

        gym.getMembers().add(user);
        user.getGyms().add(gym);

        gym.getTariffs().add(tariff);

        userRepository.save(user);
        gymRepository.save(gym);
        return tariffRepository.save(tariff);
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(UUID id) {
        return tariffRepository.findById(id)
                .map(tariff -> {
                    if (tariff.getUsers() != null) {
                        for (User user : tariff.getUsers()) {
                            user.getTariffs().remove(tariff);
                        }
                        userRepository.saveAll(tariff.getUsers());
                    }

                    if (tariff.getGym() != null) {
                        tariff.getGym().getTariffs().remove(tariff);
                        gymRepository.save(tariff.getGym());
                    }

                    tariffRepository.delete(tariff);
                    return ResponseEntity.ok("Tariff deleted");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UUID id, Tariff tariff) {
        return tariffRepository.findById(id)
                .map(existingTariff -> {
                    existingTariff.setName(tariff.getName());
                    existingTariff.setPrice(tariff.getPrice());
                    existingTariff.setEndDate(tariff.getEndDate());
                    existingTariff.setDuration(tariff.getDuration());

                    if (tariff.getGym() != null) {
                        Gym newGym = gymRepository.findById(tariff.getGym().getId())
                                .orElseThrow(() -> new RuntimeException("Gym not found"));
                        if (existingTariff.getGym() != null) {
                            existingTariff.getGym().getTariffs().remove(existingTariff);
                            gymRepository.save(existingTariff.getGym());
                        }
                        existingTariff.setGym(newGym);
                        newGym.getTariffs().add(existingTariff);
                        gymRepository.save(newGym);
                    } else if (existingTariff.getGym() != null) {
                        existingTariff.getGym().getTariffs().remove(existingTariff);
                        gymRepository.save(existingTariff.getGym());
                        existingTariff.setGym(null);
                    }

                    return ResponseEntity.ok(tariffRepository.save(existingTariff));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
