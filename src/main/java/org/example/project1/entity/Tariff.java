package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tariff")
@Builder
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private Double price;
    private Date endDate;
    private Integer duration;
    private String description;

    @ManyToOne
    @JoinColumn(name = "gym_id")
    @JsonIgnoreProperties("tariffs")
    private Gym gym;

    @ManyToMany(mappedBy = "tariffs", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("tariffs")
    private List<User> users = new ArrayList<>();

}
