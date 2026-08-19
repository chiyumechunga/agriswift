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
@RequestMapping("/api/v1/auth/farmer")
class FarmerAuthController {

    private final AuthService farmerAuthService;

    public FarmerAuthController(@Qualifier("farmerAuthService") AuthService farmerAuthService) {
        this.farmerAuthService = farmerAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(farmerAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return ResponseEntity.ok(farmerAuthService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SecurityUser user) {
        farmerAuthService.logout(user.getUserPrincipal().id());
        return ResponseEntity.noContent().build();
    }
}