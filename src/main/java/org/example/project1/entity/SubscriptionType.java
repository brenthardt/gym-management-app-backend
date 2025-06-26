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
@Table(name = "subscription_type")
@Builder
public class SubscriptionType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private Double price;
    private Integer duration;
    private String description;
    private String type;

    @ManyToOne
    @JoinColumn(name = "gym_id")
    @JsonIgnoreProperties("tariffs")
    private Gym gym;

    @ManyToMany(mappedBy = "subscriptionTypes", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("subscriptionTypes")
    private List<User> users = new ArrayList<>();


    public SubscriptionType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
