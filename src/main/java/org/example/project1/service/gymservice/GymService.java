package org.example.project1.service;


import org.example.project1.dto.GymDto;
import org.example.project1.dto.LoginDto;
import org.example.project1.dto.UserDto;
import org.example.project1.entity.Gym;
import org.example.project1.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface GymService {
    ResponseEntity<?> findAll();
    Gym save(Gym gym);
    ResponseEntity<?> delete(UUID id);
ResponseEntity<?> update(UUID id, Gym gym);


}
