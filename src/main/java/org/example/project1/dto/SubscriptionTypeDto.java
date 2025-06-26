package org.example.project1.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SubscriptionTypeDto {
    private UUID id;
    private String name;
    private Double price;
    private Integer duration;
    private String description;
    private String type;
    private UUID gymId;
}
