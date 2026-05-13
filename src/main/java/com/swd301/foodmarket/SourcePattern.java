package com.swd301.foodmarket;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
@EnableScheduling
@SpringBootApplication
public class SourcePattern {

    public static void main(String[] args) {
        SpringApplication.run(SourcePattern.class, args);
    }

}
 