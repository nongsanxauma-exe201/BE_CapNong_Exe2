package com.swd301.foodmarket.service.impl;

import com.swd301.foodmarket.dto.request.MailBody;
import com.swd301.foodmarket.entity.OtpVerification;
import com.swd301.foodmarket.repository.OtpVerificationRepository;
import com.swd301.foodmarket.service.OtpVerificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpVerificationServiceImpl implements OtpVerificationService {

    JavaMailSender javaMailSender;
    OtpVerificationRepository otpVerificationRepository;

    public String sendOtp(String email) {
        // Tạo OTP
        int otp = otpGenerator();
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + 2 * 60 * 1000);

        // Lưu OTP vào database
        Optional<OtpVerification> existingOtp = otpVerificationRepository.findByEmail(email);
        OtpVerification otpVerification;

        if (existingOtp.isPresent()) {
            otpVerification = existingOtp.get();
            otpVerification.setOtp(otp);
            otpVerification.setCreatedAt(now);
            otpVerification.setExpirationTime(expirationTime);
        } else {
            otpVerification = OtpVerification.builder()
                    .email(email)
                    .otp(otp)
                    .expirationTime(expirationTime)
                    .createdAt(now)
                    .build();
        }

        otpVerificationRepository.save(otpVerification);

        // Đọc HTML template & replace OTP
        String html;
        try {
            Resource resource = new ClassPathResource("templates/otp_template.html");

            InputStream inputStream = resource.getInputStream();
            String htmlTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            html = htmlTemplate.replace("${otp}", String.valueOf(otp));
        } catch (IOException e) {
            throw new RuntimeException("Không đọc được file template OTP email", e);
        }

        // Gửi email HTML
        MailBody mailBody = MailBody.builder()
                .to(email)
                .subject("OTP cho xác thực tài khoản")
                .text(html)
                .build();
        sendSimpleMessage(mailBody);

        return "OTP đã được gửi đến email!";
    }

    public String verifyOtp(Integer otp, String email) {
        OtpVerification otpVerification = otpVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy OTP cho email: " + email));

        if (otpVerification.getExpirationTime().before(new Date())) {
            throw new RuntimeException("OTP đã hết hạn.");
        }

        if (!otpVerification.getOtp().equals(otp)) {
            throw new RuntimeException("OTP không đúng.");
        }

        return "OTP xác thực thành công!";
    }

    private void sendSimpleMessage(MailBody mailBody) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(mailBody.to());
            helper.setFrom("vinhhien8882004@gmail.com");
            helper.setSubject(mailBody.subject());
            helper.setText(mailBody.text(), true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Gửi email thất bại", e);
        }
    }

    @Override
    @Transactional
    public void deleteOtpByEmail(String email) {
        otpVerificationRepository.deleteByEmail(email);
    }

    private Integer otpGenerator() {
        Random random = new Random();
        return random.nextInt(100_000, 999_999);
    }
}
