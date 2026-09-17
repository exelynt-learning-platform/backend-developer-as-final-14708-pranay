package com.example.resourcebooking.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
            select count(r) > 0 from Reservation r
            where r.resource.id = :resourceId
              and r.status <> com.example.resourcebooking.reservation.ReservationStatus.CANCELLED
              and (:excludeId is null or r.id <> :excludeId)
              and :startTime < r.endTime
              and :endTime > r.startTime
            """)
    boolean existsActiveOverlap(@Param("resourceId") Long resourceId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("excludeId") Long excludeId);

    @Query("""
            select r from Reservation r
            where (:userId is null or r.user.id = :userId)
              and (:status is null or r.status = :status)
              and (:minPrice is null or r.totalPrice >= :minPrice)
              and (:maxPrice is null or r.totalPrice <= :maxPrice)
            """)
    Page<Reservation> search(@Param("userId") Long userId,
                             @Param("status") ReservationStatus status,
                             @Param("minPrice") BigDecimal minPrice,
                             @Param("maxPrice") BigDecimal maxPrice,
                             Pageable pageable);
}
