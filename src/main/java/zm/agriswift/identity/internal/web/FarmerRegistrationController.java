package zm.agriswift.identity.internal.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.identity.internal.service.FarmerRegisterRequest;
import zm.agriswift.identity.internal.service.RegistrationOrchestrator;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/farmer")
@RequiredArgsConstructor
public class FarmerRegistrationController {

    private final RegistrationOrchestrator registrationOrchestrator;

    @PostMapping("/register")
    public ResponseEntity<UUID> registerFarmer(@Valid @RequestBody FarmerRegisterRequest request) {
        UUID farmerId = registrationOrchestrator.registerFarmer(request);
        return ResponseEntity.ok(farmerId);
    }
}