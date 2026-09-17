package com.example.resourcebooking.reservation;

import com.example.resourcebooking.auth.CurrentUser;
import com.example.resourcebooking.common.BusinessRuleException;
import com.example.resourcebooking.common.ForbiddenException;
import com.example.resourcebooking.common.NotFoundException;
import com.example.resourcebooking.common.SortUtil;
import com.example.resourcebooking.resource.Resource;
import com.example.resourcebooking.resource.ResourceService;
import com.example.resourcebooking.user.UserAccount;
import com.example.resourcebooking.user.UserRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "startTime", "endTime", "status", "totalPrice", "createdAt");
    private final ReservationRepository reservations;
    private final ResourceService resourceService;
    private final UserRepository users;

    public ReservationService(ReservationRepository reservations, ResourceService resourceService, UserRepository users) {
        this.reservations = reservations;
        this.resourceService = resourceService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> findAll(CurrentUser currentUser, ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                             int page, int size, String sort) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessRuleException("minPrice must be less than or equal to maxPrice");
        }
        Long userId = currentUser.isAdmin() ? null : currentUser.id();
        return reservations.search(userId, status, minPrice, maxPrice, SortUtil.page(page, size, sort, SORT_FIELDS))
                .map(ReservationResponse::from);
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(CurrentUser currentUser, Long id) {
        Reservation reservation = getOwnedOrAdmin(currentUser, id);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse create(CurrentUser currentUser, ReservationRequest request) {
        validateTimes(request);
        UserAccount user = users.findById(currentUser.id()).orElseThrow(() -> new NotFoundException("User not found"));
        Resource resource = resourceService.get(request.resourceId());
        ReservationStatus status = request.status() == null ? ReservationStatus.PENDING : request.status();
        ensureNoOverlap(resource.getId(), request.startTime(), request.endTime(), null, status);
        return ReservationResponse.from(reservations.save(new Reservation(resource, user, request.startTime(), request.endTime(), status)));
    }

    @Transactional
    public ReservationResponse update(CurrentUser currentUser, Long id, ReservationRequest request) {
        validateTimes(request);
        Reservation reservation = getOwnedOrAdmin(currentUser, id);
        Resource resource = resourceService.get(request.resourceId());
        ReservationStatus status = request.status() == null ? reservation.getStatus() : request.status();
        ensureNoOverlap(resource.getId(), request.startTime(), request.endTime(), id, status);
        reservation.update(resource, request.startTime(), request.endTime(), status);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public void delete(CurrentUser currentUser, Long id) {
        reservations.delete(getOwnedOrAdmin(currentUser, id));
    }

    private Reservation getOwnedOrAdmin(CurrentUser currentUser, Long id) {
        Reservation reservation = reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
        if (!currentUser.isAdmin() && !reservation.getUser().getId().equals(currentUser.id())) {
            throw new ForbiddenException("Reservation belongs to another user");
        }
        return reservation;
    }

    private void validateTimes(ReservationRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("endTime must be after startTime");
        }
    }

    private void ensureNoOverlap(Long resourceId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, Long excludeId, ReservationStatus status) {
        if (status != ReservationStatus.CANCELLED && reservations.existsActiveOverlap(resourceId, startTime, endTime, excludeId)) {
            throw new BusinessRuleException("Reservation overlaps an active reservation for this resource");
        }
    }
}
