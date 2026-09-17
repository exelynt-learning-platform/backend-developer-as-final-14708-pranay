package com.example.resourcebooking.reservation;

import com.example.resourcebooking.resource.Resource;
import com.example.resourcebooking.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Resource resource;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private UserAccount user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Reservation() {
    }

    public Reservation(Resource resource, UserAccount user, LocalDateTime startTime, LocalDateTime endTime, ReservationStatus status) {
        this.resource = resource;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalPrice = resource.getPrice();
    }

    public Long getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public UserAccount getUser() {
        return user;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(Resource resource, LocalDateTime startTime, LocalDateTime endTime, ReservationStatus status) {
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.totalPrice = resource.getPrice();
    }
}
