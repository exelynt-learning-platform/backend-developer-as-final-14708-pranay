package com.example.resourcebooking.auth;

import com.example.resourcebooking.user.Role;

public record CurrentUser(Long id, String username, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
