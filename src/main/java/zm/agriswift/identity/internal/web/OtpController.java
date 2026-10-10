package zm.agriswift.identity.internal.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.identity.internal.service.OtpService;

@RestController
@RequestMapping("/api/v1/auth/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    // Triggered when farmer clicks "Send free OTP SMS"
    @PostMapping("/send")
    public ResponseEntity<Void> sendOtp(@RequestParam String phone) {
        otpService.generateAndSendOtp(phone);
        return ResponseEntity.ok().build();
    }

    // Note: Verification usually happens inside the LoginRequest payload
    // if the user chooses the OTP authMode instead of the PIN.
}