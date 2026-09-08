package com.booking.config;

import com.booking.domain.entity.Resource;
import com.booking.domain.entity.User;
import com.booking.domain.enums.Role;
import com.booking.domain.repository.ResourceRepository;
import com.booking.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedData(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(new User(
                        "admin",
                        "admin@booking.local",
                        passwordEncoder.encode("Admin@123"),
                        Role.ADMIN
                ));
                log.info("Seeded ADMIN user: admin / Admin@123");
            }

            if (!userRepository.existsByUsername("user")) {
                userRepository.save(new User(
                        "user",
                        "user@booking.local",
                        passwordEncoder.encode("User@123"),
                        Role.USER
                ));
                log.info("Seeded USER account: user / User@123");
            }

            if (!userRepository.existsByUsername("alice")) {
                userRepository.save(new User(
                        "alice",
                        "alice@booking.local",
                        passwordEncoder.encode("Alice@123"),
                        Role.USER
                ));
                log.info("Seeded USER account: alice / Alice@123");
            }

            if (resourceRepository.count() == 0) {
                resourceRepository.save(new Resource(
                        "Conference Room A",
                        "ROOM",
                        "Seats 12 with projector and whiteboard",
                        new BigDecimal("75.00"),
                        true
                ));
                resourceRepository.save(new Resource(
                        "Company Van",
                        "VEHICLE",
                        "7-seater van for local trips",
                        new BigDecimal("120.50"),
                        true
                ));
                resourceRepository.save(new Resource(
                        "DSLR Camera Kit",
                        "EQUIPMENT",
                        "Full-frame camera with 24-70mm lens",
                        new BigDecimal("45.00"),
                        true
                ));
                log.info("Seeded sample bookable resources");
            }
        };
    }
}
