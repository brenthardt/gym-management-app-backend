package org.example.project1.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;
@Data
public class SubscriptionDto {
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer price;
    private boolean status;
    private Integer duration;
    private UserDto user;
    private SubscriptionTypeDto subscriptionType;
    private String name;
}
