package com.swd301.foodmarket.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "otp_verifications")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer otp;

    @Column(nullable = false)
    private Date expirationTime;

    @Column(nullable = false)
    private Date createdAt;
}
