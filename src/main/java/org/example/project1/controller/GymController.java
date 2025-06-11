package org.example.project1.controller;



import lombok.RequiredArgsConstructor;
import org.example.project1.dto.GymDto;
import org.example.project1.entity.Gym;
import org.example.project1.service.GymService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/gym")
@RequiredArgsConstructor
public class GymController {

    private final GymService gymService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        return gymService.findAll();
    }

    @PostMapping
    public Gym save(@RequestBody Gym gym) {
        return gymService.save(gym);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return gymService.delete(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,@RequestBody Gym gym) {
        return gymService.update(id, gym);
    }




}
