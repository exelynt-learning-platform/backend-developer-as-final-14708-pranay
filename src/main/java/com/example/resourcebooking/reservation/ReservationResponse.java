package com.example.resourcebooking.reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long userId,
        String username,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,
        BigDecimal totalPrice,
        Instant createdAt
) {
    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt()
        );
    }
}
