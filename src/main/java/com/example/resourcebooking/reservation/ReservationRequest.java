package com.example.resourcebooking.reservation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull Long resourceId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        ReservationStatus status
) {
}
