package com.booking.service;

import com.booking.domain.entity.Reservation;
import com.booking.domain.entity.Resource;
import com.booking.domain.entity.User;
import com.booking.domain.enums.ReservationStatus;
import com.booking.domain.enums.Role;
import com.booking.domain.repository.ReservationRepository;
import com.booking.domain.repository.UserRepository;
import com.booking.dto.request.ReservationRequest;
import com.booking.exception.ForbiddenException;
import com.booking.mapper.ReservationMapper;
import com.booking.security.SecurityUtils;
import com.booking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ResourceService resourceService;
    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ReservationService reservationService;

    private User owner;
    private Resource resource;

    @BeforeEach
    void setUp() {
        owner = new User("user", "user@test.com", "hash", Role.USER);
        owner.setId(10L);
        resource = new Resource("Room", "ROOM", "desc", new BigDecimal("50.00"), true);
        resource.setId(1L);
    }

    @Test
    void createUsesJwtUserIdentityNotRequestBody() {
        when(securityUtils.currentUser()).thenReturn(UserPrincipal.from(owner));
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(resourceService.getResource(1L)).thenReturn(resource);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(reservationMapper.toResponse(any(Reservation.class))).thenReturn(null);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setPrice(new BigDecimal("50.00"));

        reservationService.create(request);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void userCannotAccessAnotherUsersReservation() {
        User other = new User("alice", "alice@test.com", "hash", Role.USER);
        other.setId(20L);

        Reservation reservation = new Reservation();
        reservation.setId(5L);
        reservation.setUser(other);
        reservation.setResource(resource);

        when(reservationRepository.findDetailedById(5L)).thenReturn(Optional.of(reservation));
        when(securityUtils.currentUser()).thenReturn(UserPrincipal.from(owner));

        assertThatThrownBy(() -> reservationService.findById(5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own reservations");
    }
}
