package com.swd301.foodmarket.scheduler;

import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.UserService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserUnlockScheduler {
    UserRepository userRepository;
    UserService userService;
    JavaMailSender javaMailSender;

    @Scheduled(cron = "0 0 0 * * ?") // chạy mỗi ngày lúc 00:00
    public void autoUnlockUsers() {


        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        List<User> lockedUsers = userRepository.findUsersLockedBefore(sevenDaysAgo);

        for (User user : lockedUsers) {
            userService.activateUser(user.getId());
            sendUnlockEmail(user);

            log.info("Auto unlocked user: {}", user.getId());
        }
    }

    private void sendUnlockEmail(User user) {

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom("your_email@gmail.com");
            helper.setSubject("Tài khoản của bạn đã được mở khóa");

            String html = """
                <h2>Tài khoản đã được mở khóa</h2>
                <p>Xin chào %s,</p>
                <p>Tài khoản của bạn đã được <b>mở khóa </b> sau 7 ngày.</p>
                <p>Bạn có thể đăng nhập và tiếp tục sử dụng hệ thống.</p>
                <br>
                <p>Trân trọng,<br>FoodMarket System</p>
                """.formatted(user.getFullName());

            helper.setText(html, true);

            javaMailSender.send(mimeMessage);

            log.info("Unlock email sent to {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send unlock email to {}", user.getEmail(), e);
        }
    }
}
