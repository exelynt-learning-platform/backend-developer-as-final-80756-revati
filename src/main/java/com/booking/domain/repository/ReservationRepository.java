package com.booking.domain.repository;

import com.booking.domain.entity.Reservation;
import com.booking.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @EntityGraph(attributePaths = {"resource", "user"})
    @Query("""
            SELECT r FROM Reservation r
            WHERE (:userId IS NULL OR r.user.id = :userId)
              AND (:status IS NULL OR r.status = :status)
              AND (:minPrice IS NULL OR r.price >= :minPrice)
              AND (:maxPrice IS NULL OR r.price <= :maxPrice)
            """)
    Page<Reservation> findFiltered(
            @Param("userId") Long userId,
            @Param("status") ReservationStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"resource", "user"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findDetailedById(@Param("id") Long id);
}
