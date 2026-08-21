package zm.agriswift.identity.internal.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zm.agriswift.identity.internal.security.SecurityUser;
import zm.agriswift.identity.internal.service.AuthResponse;
import zm.agriswift.identity.internal.service.AuthService;
import zm.agriswift.identity.internal.service.LoginRequest;
import zm.agriswift.identity.internal.service.RefreshRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService staffAuthService;

    public AuthController(@Qualifier("staffAuthService") AuthService staffAuthService) {
        this.staffAuthService = staffAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(staffAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(staffAuthService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SecurityUser user) {
        staffAuthService.logout(user.getUserPrincipal().id());
        return ResponseEntity.noContent().build();
    }
}