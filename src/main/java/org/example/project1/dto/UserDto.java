package org.example.project1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private UUID id;
    private String name;
    private String password;
    private String phone;
    private List<GymDto> gyms;
    private List<SubscriptionTypeDto> subscriptionTypes;
    private SubscriptionDto subscription;
    private Integer duration;
    private Date purchaseDate;
}

