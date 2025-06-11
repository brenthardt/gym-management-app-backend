package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gym_id")
    @JsonBackReference
    private Gym gym;

    @ManyToOne
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    private Integer duration;
    private Date purchaseDate;

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
