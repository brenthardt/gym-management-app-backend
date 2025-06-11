package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.project1.type.Type;

import java.util.Date;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tariff")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private Double price;
    private Date endDate;
    private Integer duration;
    @Enumerated(EnumType.STRING)
    private Type planType;

    @ManyToOne
    @JoinColumn(name = "gym_id")
    @JsonBackReference
    private Gym gym;


}
