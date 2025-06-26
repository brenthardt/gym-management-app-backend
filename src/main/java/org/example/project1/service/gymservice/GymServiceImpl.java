package org.example.project1.service.gymservice;

import lombok.RequiredArgsConstructor;
import org.example.project1.dto.GymDto;
import org.example.project1.dto.UserDto;   
import org.example.project1.entity.Gym;
import org.example.project1.entity.User;
import org.example.project1.repository.GymRepository;
import org.example.project1.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GymServiceImpl implements GymService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;

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
    public Gym addUserToGym(UUID gymId, User user) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        gym.getMembers().add(user);
        user.getGyms().add(gym);

        userRepository.save(user);
        return gymRepository.save(gym);
    }

    @Override
    public ResponseEntity<?> delete(UUID id) {
        return gymRepository.findById(id).map(gym -> {
            if (gym.getMembers() != null && !gym.getMembers().isEmpty()) {
                for (User user : gym.getMembers()) {
                    user.getGyms().remove(gym);
                    userRepository.save(user);
                }
            }

            gymRepository.delete(gym);
            return ResponseEntity.ok("Gym deleted");
        }).orElse(ResponseEntity.notFound().build());
    }

    @Override
    @Transactional
    public ResponseEntity<?> update(UUID id, Gym gym) {
        return gymRepository.findById(id)
                .map(existingGym -> {
                    existingGym.setName(gym.getName());
                    existingGym.setLocation(gym.getLocation());

                    if (gym.getMembers() != null) {
                        if (existingGym.getMembers() != null) {
                            for (User oldMember : existingGym.getMembers()) {
                                oldMember.getGyms().remove(existingGym);
                            }
                            userRepository.saveAll(existingGym.getMembers());
                            existingGym.getMembers().clear();
                        }

                        if (!gym.getMembers().isEmpty()) {
                            List<UUID> memberIds = gym.getMembers().stream()
                                    .map(User::getId)
                                    .toList();
                            List<User> newMembers = userRepository.findAllById(memberIds);
                            for (User member : newMembers) {
                                member.getGyms().add(existingGym);
                            }
                            existingGym.setMembers(newMembers);
                            userRepository.saveAll(newMembers);
                        }
                    }

                    return ResponseEntity.ok(gymRepository.save(existingGym));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private GymDto mapToDto(Gym gym) {
        GymDto dto = new GymDto();
        dto.setId(gym.getId());
        dto.setName(gym.getName());
        dto.setLocation(gym.getLocation());

        if (gym.getMembers() != null) {
            dto.setMembers(gym.getMembers().stream().map(this::mapUserToDto).toList());
        }

        return dto;
    }

    private UserDto mapUserToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());

        return dto;
    }

}
