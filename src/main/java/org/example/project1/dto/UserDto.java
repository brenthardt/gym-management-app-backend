package org.example.project1.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String name;
    private String password;
    private String phone;
    private TariffDto plan;
    private GymDto gym;


}
