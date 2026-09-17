package com.example.resourcebooking.reservation;

import com.example.resourcebooking.auth.CurrentUser;
import com.example.resourcebooking.common.PageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public PageResponse<ReservationResponse> findAll(@AuthenticationPrincipal CurrentUser currentUser,
                                                     @RequestParam(required = false) ReservationStatus status,
                                                     @RequestParam(required = false) BigDecimal minPrice,
                                                     @RequestParam(required = false) BigDecimal maxPrice,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     @RequestParam(required = false) String sort) {
        return PageResponse.from(reservationService.findAll(currentUser, status, minPrice, maxPrice, page, size, sort));
    }

    @GetMapping("/{id}")
    public ReservationResponse findById(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        return reservationService.findById(currentUser, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@AuthenticationPrincipal CurrentUser currentUser, @Valid @RequestBody ReservationRequest request) {
        return reservationService.create(currentUser, request);
    }

    @PutMapping("/{id}")
    public ReservationResponse update(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return reservationService.update(currentUser, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable Long id) {
        reservationService.delete(currentUser, id);
    }
}
