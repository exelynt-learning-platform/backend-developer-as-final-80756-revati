package com.booking;

import com.booking.domain.entity.Reservation;
import com.booking.domain.entity.Resource;
import com.booking.domain.entity.User;
import com.booking.domain.enums.ReservationStatus;
import com.booking.domain.enums.Role;
import com.booking.domain.repository.ReservationRepository;
import com.booking.domain.repository.ResourceRepository;
import com.booking.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Resource resource;
    private User admin;
    private User user;
    private User alice;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        admin = userRepository.save(new User("admin", "admin@test.com", passwordEncoder.encode("Admin@123"), Role.ADMIN));
        user = userRepository.save(new User("user", "user@test.com", passwordEncoder.encode("User@123"), Role.USER));
        alice = userRepository.save(new User("alice", "alice@test.com", passwordEncoder.encode("Alice@123"), Role.USER));
        resource = resourceRepository.save(new Resource(
                "Room A", "ROOM", "Test room", new BigDecimal("100.00"), true
        ));
    }

    @Test
    void loginReturnsJwt() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void loginFailsWithBadCredentials() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateResource() throws Exception {
        String token = login("user", "User@123");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Secret Room",
                                  "type":"ROOM",
                                  "description":"nope",
                                  "basePrice":10.00,
                                  "available":true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateResourceAndUserCanRead() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String userToken = login("user", "User@123");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Bike",
                                  "type":"VEHICLE",
                                  "description":"City bike",
                                  "basePrice":15.50,
                                  "available":true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bike"));

        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void userSeesOnlyOwnReservationsWhileAdminSeesAll() throws Exception {
        Reservation userReservation = new Reservation();
        userReservation.setUser(user);
        userReservation.setResource(resource);
        userReservation.setStartTime(LocalDateTime.now().plusDays(1));
        userReservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        userReservation.setPrice(new BigDecimal("80.00"));
        userReservation.setStatus(ReservationStatus.PENDING);
        reservationRepository.save(userReservation);

        Reservation aliceReservation = new Reservation();
        aliceReservation.setUser(alice);
        aliceReservation.setResource(resource);
        aliceReservation.setStartTime(LocalDateTime.now().plusDays(2));
        aliceReservation.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        aliceReservation.setPrice(new BigDecimal("120.00"));
        aliceReservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(aliceReservation);

        String userToken = login("user", "User@123");
        String adminToken = login("admin", "Admin@123");

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("user"));

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "CONFIRMED")
                        .param("minPrice", "100")
                        .param("maxPrice", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    void createReservationBindsOwnerFromJwt() throws Exception {
        String token = login("user", "User@123");
        LocalDateTime start = LocalDateTime.now().plusDays(3).withNano(0);
        LocalDateTime end = start.plusHours(3);

        String body = """
                {
                  "resourceId": %d,
                  "startTime": "%s",
                  "endTime": "%s",
                  "price": 99.99,
                  "notes": "Team sync"
                }
                """.formatted(resource.getId(), start, end);

        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("price").asDouble()).isEqualTo(99.99);
    }

    @Test
    void userCannotDeleteReservation() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(LocalDateTime.now().plusDays(1));
        reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        reservation.setPrice(new BigDecimal("40.00"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation = reservationRepository.save(reservation);

        String token = login("user", "User@123");

        mockMvc.perform(delete("/api/reservations/" + reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationRejectsInvalidReservation() throws Exception {
        String token = login("user", "User@123");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": null,
                                  "startTime": "2020-01-01T10:00:00",
                                  "endTime": "2020-01-01T09:00:00",
                                  "price": -5
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanUpdateResource() throws Exception {
        String token = login("admin", "Admin@123");

        mockMvc.perform(put("/api/resources/" + resource.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Room A Updated",
                                  "type":"ROOM",
                                  "description":"Updated",
                                  "basePrice":110.00,
                                  "available":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Room A Updated"))
                .andExpect(jsonPath("$.available").value(false));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
