package com.example.resourcebooking.config;

import com.example.resourcebooking.resource.Resource;
import com.example.resourcebooking.resource.ResourceRepository;
import com.example.resourcebooking.user.Role;
import com.example.resourcebooking.user.UserAccount;
import com.example.resourcebooking.user.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean
    @Profile("!test")
    CommandLineRunner seed(UserRepository users, ResourceRepository resources, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!users.existsByUsername("admin")) {
                users.save(new UserAccount("admin", passwordEncoder.encode("admin123"), Role.ADMIN));
            }
            if (!users.existsByUsername("user")) {
                users.save(new UserAccount("user", passwordEncoder.encode("user123"), Role.USER));
            }
            if (resources.count() == 0) {
                resources.save(new Resource("Conference Room A", "Large room with projector", new BigDecimal("120.00")));
                resources.save(new Resource("Shared Desk", "Hot desk near a window", new BigDecimal("25.00")));
            }
        };
    }
}
