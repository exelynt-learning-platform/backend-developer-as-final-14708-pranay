package com.example.resourcebooking;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.resourcebooking.resource.Resource;
import com.example.resourcebooking.resource.ResourceRepository;
import com.example.resourcebooking.reservation.ReservationRepository;
import com.example.resourcebooking.user.Role;
import com.example.resourcebooking.user.UserAccount;
import com.example.resourcebooking.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceBookingIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository users;

    @Autowired
    ResourceRepository resources;

    @Autowired
    ReservationRepository reservations;

    @Autowired
    PasswordEncoder passwordEncoder;

    Long roomId;
    Long deskId;

    @BeforeEach
    void setUp() {
        reservations.deleteAll();
        resources.deleteAll();
        users.deleteAll();
        users.save(new UserAccount("admin", passwordEncoder.encode("admin123"), Role.ADMIN));
        users.save(new UserAccount("user", passwordEncoder.encode("user123"), Role.USER));
        users.save(new UserAccount("other", passwordEncoder.encode("other123"), Role.USER));
        roomId = resources.save(new Resource("Room", "Conference room", new BigDecimal("100.00"))).getId();
        deskId = resources.save(new Resource("Desk", "Shared desk", new BigDecimal("25.00"))).getId();
    }

    @Test
    void loginReturnsJwtAndRejectsBadCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "user", "password", "user123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", containsString(".")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "user", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void protectedEndpointsReturnStructuredUnauthorizedErrors() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(get("/resources").header("Authorization", "Bearer bad.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void resourceCrudIsAdminOnlyAndUserCanRead() throws Exception {
        String adminToken = login("admin", "admin123");
        String userToken = login("user", "user123");

        mockMvc.perform(get("/resources").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        mockMvc.perform(post("/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(resource("Projector", "4k projector", "15.00"))))
                .andExpect(status().isForbidden());

        JsonNode created = read(mockMvc.perform(post("/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(resource("Projector", "4k projector", "15.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Projector"))
                .andReturn().getResponse().getContentAsString());

        Long id = created.get("id").asLong();
        mockMvc.perform(put("/resources/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(resource("Updated", "Updated desc", "20.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(20.00));

        mockMvc.perform(delete("/resources/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/resources?sort=unknown,asc").header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported sort field")));
    }

    @Test
    void reservationCreationUsesJwtAndPreventsOverlappingActiveReservations() throws Exception {
        String userToken = login("user", "user123");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);

        JsonNode created = createReservation(userToken, roomId, start, start.plusHours(2), "PENDING", status().isCreated());
        mockMvc.perform(get("/reservations/" + created.get("id").asLong()).header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));

        createReservation(userToken, roomId, start.plusMinutes(30), start.plusHours(3), "CONFIRMED", status().isBadRequest())
                .path("message").asText().contains("overlaps");

        createReservation(userToken, roomId, start.plusMinutes(30), start.plusHours(3), "CANCELLED", status().isCreated());

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("resourceId", roomId, "startTime", start.plusHours(5).toString(), "endTime", start.plusHours(4).toString()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("endTime must be after startTime"));
    }

    @Test
    void usersOnlyAccessOwnReservationsWhileAdminCanAccessAll() throws Exception {
        String userToken = login("user", "user123");
        String otherToken = login("other", "other123");
        String adminToken = login("admin", "admin123");
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);

        JsonNode own = createReservation(userToken, roomId, start, start.plusHours(1), "PENDING", status().isCreated());
        JsonNode others = createReservation(otherToken, deskId, start, start.plusHours(1), "PENDING", status().isCreated());

        mockMvc.perform(get("/reservations/" + others.get("id").asLong()).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/reservations").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/reservations").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(put("/reservations/" + own.get("id").asLong())
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reservation(roomId, start.plusHours(2), start.plusHours(3), "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(delete("/reservations/" + others.get("id").asLong()).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/reservations/" + others.get("id").asLong()).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void filteringPaginationSortingValidationAndErrorResponsesWork() throws Exception {
        String userToken = login("user", "user123");
        LocalDateTime start = LocalDateTime.now().plusDays(4).withNano(0);
        createReservation(userToken, roomId, start, start.plusHours(1), "CONFIRMED", status().isCreated());
        createReservation(userToken, deskId, start.plusHours(2), start.plusHours(3), "PENDING", status().isCreated());

        mockMvc.perform(get("/reservations?status=CONFIRMED&minPrice=50&maxPrice=150&page=0&size=1&sort=totalPrice,desc")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/reservations?minPrice=200&maxPrice=10").header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("minPrice must be less than or equal to maxPrice"));

        mockMvc.perform(get("/reservations?status=UNKNOWN").header("Authorization", bearer(userToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: status"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resourceId\":" + roomId + ",\"startTime\":\"" + start.plusDays(1) + "\",\"endTime\":\"" + start.plusDays(1).plusHours(1) + "\",\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid request body"));

        mockMvc.perform(post("/resources")
                        .header("Authorization", bearer(login("admin", "admin123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(resource("", "", "-1.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());

        mockMvc.perform(get("/resources/999").header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found: 999"));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return read(body).get("token").asText();
    }

    private JsonNode createReservation(String token, Long resourceId, LocalDateTime start, LocalDateTime end, String status,
                                       org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        String body = mockMvc.perform(post("/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(reservation(resourceId, start, end, status))))
                .andExpect(expectedStatus)
                .andReturn().getResponse().getContentAsString();
        return read(body);
    }

    private Map<String, Object> resource(String name, String description, String price) {
        return Map.of("name", name, "description", description, "price", new BigDecimal(price));
    }

    private Map<String, Object> reservation(Long resourceId, LocalDateTime start, LocalDateTime end, String status) {
        return Map.of("resourceId", resourceId, "startTime", start.toString(), "endTime", end.toString(), "status", status);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(String value) throws Exception {
        return objectMapper.readTree(value);
    }
}
