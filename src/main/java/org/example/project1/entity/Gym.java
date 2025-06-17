package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "gyms")
@Builder
public class Gym {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String location;

    @OneToMany(mappedBy = "gym")
    private List<Tariff> tariffs;

    @ManyToMany(mappedBy = "gyms", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("gyms")
    private List<User> members = new ArrayList<>();
}
