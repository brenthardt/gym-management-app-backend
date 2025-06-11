package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.project1.type.Type;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "gym")
public class Gym {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String location;

    @OneToMany( fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id")
    private List<Tariff> plans;

    @OneToMany( fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    @JsonManagedReference
    private List<User> members;
}
