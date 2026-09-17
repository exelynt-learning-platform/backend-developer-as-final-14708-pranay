package com.example.resourcebooking.resource;

import java.math.BigDecimal;

public record ResourceResponse(Long id, String name, String description, BigDecimal price) {
    static ResourceResponse from(Resource resource) {
        return new ResourceResponse(resource.getId(), resource.getName(), resource.getDescription(), resource.getPrice());
    }
}
