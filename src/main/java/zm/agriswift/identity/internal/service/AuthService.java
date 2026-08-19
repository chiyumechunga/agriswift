package zm.agriswift.identity.internal.service;

import zm.agriswift.identity.api.dto.UserPrincipal;
import zm.agriswift.identity.internal.service.RefreshRequest;
import zm.agriswift.identity.internal.service.LoginRequest;
import zm.agriswift.identity.internal.service.AuthResponse;

import java.util.UUID;

// AuthService.java — interface
public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(UUID userId);
}
