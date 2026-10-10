package zm.agriswift.identity.internal.integration.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("AgriSwift FRA <noreply@agriswift.zm>");
        message.setTo(toEmail);
        message.setSubject("Your AgriSwift Verification Code");

        String body = String.format(
                """
                        Your secure verification code is: %s
                        
                        This code will expire in 5 minutes. If you did not request this, please ignore this email.
                        
                        AgriSwift - Zambia Food Reserve Agency Alternative""",
                otp
        );
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("OTP email dispatched to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Email delivery failed. Please try again later.");
        }
    }
}