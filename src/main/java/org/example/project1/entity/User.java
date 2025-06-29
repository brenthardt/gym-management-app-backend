package org.example.project1.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String password;
    @Column(unique = true)
    private String phone;
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Role> roles;

    private Long telegramChatId;

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
            name = "user_subscription_type",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "subscription_type_id")
    )
    @JsonIgnoreProperties("users")
    private List<SubscriptionType> subscriptionTypes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("user")
    private List<Subscription> subscriptions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private BotStep step;

    private String tempData;
    
    @Column(name = "bot_blocked")
    private Boolean botBlocked = false;

    private LocalDate lastDayCountIncrement;

    public void addGym(Gym gym) {
        this.gyms.add(gym);
        gym.getMembers().add(this);
    }

    public void addSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionTypes.add(subscriptionType);
        subscriptionType.getUsers().add(this);
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
