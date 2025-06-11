package org.example.project1.controller;



import lombok.RequiredArgsConstructor;
import org.example.project1.entity.Tariff;
import org.example.project1.service.tariffservice.TariffService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tariff")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        return tariffService.findAll();
    }

    @PostMapping
    public Tariff save(@RequestBody Tariff tariff) {
        return tariffService.save(tariff);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        return tariffService.delete(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id,@RequestBody Tariff tariff) {
        return tariffService.update(id, tariff);
    }




}
