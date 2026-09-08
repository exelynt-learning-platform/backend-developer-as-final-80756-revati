package com.booking.service;

import com.booking.domain.entity.Reservation;
import com.booking.domain.entity.Resource;
import com.booking.domain.entity.User;
import com.booking.domain.enums.ReservationStatus;
import com.booking.domain.enums.Role;
import com.booking.domain.repository.ReservationRepository;
import com.booking.domain.repository.UserRepository;
import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.ReservationUpdateRequest;
import com.booking.dto.response.PageResponse;
import com.booking.dto.response.ReservationResponse;
import com.booking.exception.BadRequestException;
import com.booking.exception.ForbiddenException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.mapper.ReservationMapper;
import com.booking.security.SecurityUtils;
import com.booking.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ResourceService resourceService;
    private final ReservationMapper reservationMapper;
    private final SecurityUtils securityUtils;

    public ReservationService(
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            ResourceService resourceService,
            ReservationMapper reservationMapper,
            SecurityUtils securityUtils
    ) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.resourceService = resourceService;
        this.reservationMapper = reservationMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> findAll(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort
    ) {
        validatePriceRange(minPrice, maxPrice);
        UserPrincipal current = securityUtils.currentUser();
        Long userIdFilter = current.getRole() == Role.ADMIN ? null : current.getId();

        Pageable pageable = buildPageable(page, size, sort);
        Page<Reservation> result = reservationRepository.findFiltered(
                userIdFilter, status, minPrice, maxPrice, pageable
        );

        return new PageResponse<>(
                result.map(reservationMapper::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        Reservation reservation = getReservation(id);
        assertCanAccess(reservation);
        return reservationMapper.toResponse(reservation);
    }

    public ReservationResponse create(ReservationRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.getResource(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new BadRequestException("Resource is not available for booking");
        }

        // Ownership is always taken from the JWT principal — never from the request body.
        UserPrincipal principal = securityUtils.currentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Reservation reservation = new Reservation();
        reservation.setResource(resource);
        reservation.setUser(user);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setNotes(request.getNotes());

        if (principal.getRole() == Role.ADMIN && request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        } else {
            reservation.setStatus(ReservationStatus.PENDING);
        }

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse update(Long id, ReservationUpdateRequest request) {
        Reservation reservation = getReservation(id);
        assertCanModify(reservation);

        UserPrincipal principal = securityUtils.currentUser();

        if (request.getResourceId() != null) {
            if (principal.getRole() != Role.ADMIN) {
                throw new ForbiddenException("Only ADMIN can change the booked resource");
            }
            reservation.setResource(resourceService.getResource(request.getResourceId()));
        }

        LocalDateTime start = request.getStartTime() != null ? request.getStartTime() : reservation.getStartTime();
        LocalDateTime end = request.getEndTime() != null ? request.getEndTime() : reservation.getEndTime();
        if (request.getStartTime() != null || request.getEndTime() != null) {
            validateTimeRange(start, end);
            reservation.setStartTime(start);
            reservation.setEndTime(end);
        }

        if (request.getPrice() != null) {
            if (principal.getRole() != Role.ADMIN) {
                throw new ForbiddenException("Only ADMIN can change reservation price");
            }
            reservation.setPrice(request.getPrice());
        }

        if (request.getStatus() != null) {
            if (principal.getRole() != Role.ADMIN
                    && request.getStatus() != ReservationStatus.CANCELLED) {
                throw new ForbiddenException("USER may only cancel their own reservations");
            }
            reservation.setStatus(request.getStatus());
        }

        if (request.getNotes() != null) {
            reservation.setNotes(request.getNotes());
        }

        reservation.setUpdatedAt(Instant.now());
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    public void delete(Long id) {
        Reservation reservation = getReservation(id);
        if (!securityUtils.isAdmin()) {
            throw new ForbiddenException("Only ADMIN can delete reservations");
        }
        reservationRepository.delete(reservation);
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertCanAccess(Reservation reservation) {
        UserPrincipal principal = securityUtils.currentUser();
        if (principal.getRole() != Role.ADMIN
                && !reservation.getUser().getId().equals(principal.getId())) {
            throw new ForbiddenException("You can only access your own reservations");
        }
    }

    private void assertCanModify(Reservation reservation) {
        assertCanAccess(reservation);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum price cannot be negative");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum price cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price");
        }
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
