package org.example.project1.controller;

import lombok.RequiredArgsConstructor;
import org.example.project1.entity.SubscriptionType;
import org.example.project1.service.subscriptionType.SubscriptionTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/subscription-type")
@RequiredArgsConstructor
public class SubscriptionTypeController {
    private final SubscriptionTypeService subscriptionTypeService;

    @GetMapping
    public ResponseEntity<?> findAll() {
        return ResponseEntity.ok(subscriptionTypeService.findAll());
    }

    @PostMapping
    public SubscriptionType save(@RequestBody SubscriptionType subscriptionType) {
        return subscriptionTypeService.save(subscriptionType);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        subscriptionTypeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody SubscriptionType subscriptionType) {
        return ResponseEntity.ok(subscriptionTypeService.update(id, subscriptionType));
    }
}
