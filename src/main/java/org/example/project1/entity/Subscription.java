package org.example.project1.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscriptions")
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer dayCount;
    private boolean status;
    private boolean limited;
    private Double price;
    private LocalDate lastDayUsed;
    private LocalDate purchaseDate;
    private LocalDate lastDecrementDate;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "subscription_type_id")
    private SubscriptionType subscriptionType;

    public LocalDate getLastDecrementDate() {
        return lastDecrementDate;
    }

    public void setLastDecrementDate(LocalDate lastDecrementDate) {
        this.lastDecrementDate = lastDecrementDate;
    }
}
