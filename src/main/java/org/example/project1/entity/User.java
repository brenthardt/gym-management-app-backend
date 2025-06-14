package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String password;
    private String phone;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;
    @Column(length = 1000)
    private String refreshToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_gym",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "gym_id")
    )
    @JsonIgnoreProperties("members")
    private List<Gym> gyms = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_tariff",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tariff_id")
    )
    @JsonIgnoreProperties("users")
    private List<Tariff> tariffs = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    private Date purchaseDate;

    public void addGym(Gym gym) {
        this.gyms.add(gym);
        gym.getMembers().add(this);
    }

    public void addTariff(Tariff tariff) {
        this.tariffs.add(tariff);
        tariff.getUsers().add(this);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getUsername() {
        return phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
