package org.example.project1.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GymDto {
    private UUID id;
    private String name;
    private String location;
    private List<TariffDto> plans;
    private List<UserDto> members;
}
