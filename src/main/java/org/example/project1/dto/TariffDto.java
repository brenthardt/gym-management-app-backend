package org.example.project1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.example.project1.type.Type;

import java.util.Date;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TariffDto {
    private UUID id;
    private String name;
    private Double price;
    private Date endDate;
    private Integer duration;
    private GymDto gym;
}
