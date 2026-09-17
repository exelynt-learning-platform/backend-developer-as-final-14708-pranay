package com.example.resourcebooking.common;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SortUtil {
    private SortUtil() {
    }

    public static Pageable page(int page, int size, String sort, Set<String> allowedFields) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!allowedFields.contains(field)) {
            throw new IllegalArgumentException("Unsupported sort field: " + field);
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported sort direction: " + parts[1]));
        }
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
