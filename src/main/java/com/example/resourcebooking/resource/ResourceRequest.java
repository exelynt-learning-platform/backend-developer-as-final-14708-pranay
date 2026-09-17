package com.example.resourcebooking.resource;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ResourceRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price
) {
}
