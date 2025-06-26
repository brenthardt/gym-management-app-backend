package org.example.project1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GymDto {
    private UUID id;
    private String name;
    private String location;
    private List<SubscriptionTypeDto> subscriptionTypes;
    private List<UserDto> members;
}
