package org.example.project1.impl;

import lombok.RequiredArgsConstructor;
import org.example.project1.dto.GymDto;
import org.example.project1.dto.TariffDto;
import org.example.project1.dto.UserDto;
import org.example.project1.entity.Gym;
import org.example.project1.entity.Tariff;
import org.example.project1.entity.User;
import org.example.project1.repository.GymRepository;
import org.example.project1.service.GymService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GymServiceImpl implements GymService {

    private final GymRepository gymRepository;

    @Override
    public ResponseEntity<?> findAll() {
        List<GymDto> gyms = gymRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
        return ResponseEntity.ok(gyms);
    }


    @Override
    public Gym save(Gym gym) {
       return gymRepository.save(gym);
    }

    @Override
    public ResponseEntity<?> delete(UUID id) {
        if (gymRepository.existsById(id)) {
            gymRepository.deleteById(id);
            return ResponseEntity.ok("Gym deleted");
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<?> update(UUID id, Gym gym) {
        return gymRepository.findById(id)
                .map(eGym -> {
                    eGym.setName(gym.getName());
                    eGym.setLocation(gym.getLocation());
                    eGym.setPlans(gym.getPlans());
                    eGym.setMembers(gym.getMembers());


                    return ResponseEntity.ok(gymRepository.save(eGym));
                })
                .orElse(ResponseEntity.notFound().build());
    }


    private GymDto mapToDto(Gym gym) {
        GymDto dto = new GymDto();
        dto.setId(gym.getId());
        dto.setName(gym.getName());
        dto.setLocation(gym.getLocation());

        if (gym.getPlans() != null) {
            dto.setPlans(gym.getPlans().stream().map(this::mapTariffToDto).toList());
        }

        if (gym.getMembers() != null) {
            dto.setMembers(gym.getMembers().stream().map(this::mapUserToDto).toList());
        }

        return dto;
    }

    private TariffDto mapTariffToDto(Tariff tariff) {
        TariffDto dto = new TariffDto();
        dto.setId(tariff.getId());
        dto.setName(tariff.getName());
        dto.setPrice(tariff.getPrice());
        dto.setEndDate(tariff.getEndDate());
        dto.setDuration(tariff.getDuration());
        dto.setPlanType(tariff.getPlanType());
        return dto;
    }

    private UserDto mapUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());

        if (user.getPlan() != null) {
            dto.setPlan(mapTariffToDto(user.getPlan()));
        }

        return dto;
    }


}
