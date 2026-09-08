package zm.agriswift.farmer.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zm.agriswift.farmer.api.FarmerDirectory;
import zm.agriswift.farmer.api.dto.FarmerSummary;
import zm.agriswift.farmer.api.dto.PreferredPayoutAccount;
import zm.agriswift.farmer.application.FarmerRegistrationService;
import zm.agriswift.farmer.application.KycService;
import zm.agriswift.farmer.application.FarmerPaymentService;
import zm.agriswift.farmer.web.dto.KycVerificationRequest;
import zm.agriswift.farmer.web.dto.RegisterFarmerRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farmers")
public class FarmerController {

    private final FarmerDirectory farmerDirectory;
    private final FarmerRegistrationService registrationService;
    private final KycService kycService;
    private final FarmerPaymentService paymentService;

    public FarmerController(FarmerDirectory farmerDirectory,
                            FarmerRegistrationService registrationService,
                            KycService kycService,
                            FarmerPaymentService paymentService) {
        this.farmerDirectory = farmerDirectory;
        this.registrationService = registrationService;
        this.kycService = kycService;
        this.paymentService = paymentService;
    }

    @GetMapping("/{farmerId}")
    public ResponseEntity<FarmerSummary> getFarmer(@PathVariable UUID farmerId) {
        return farmerDirectory.findById(farmerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UUID> registerFarmer(@RequestBody @Valid RegisterFarmerRequest request) {
        UUID farmerId = registrationService.registerFarmer(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(farmerId);
    }

    @PostMapping("/{farmerId}/kyc/verify")
    public ResponseEntity<Void> verifyKyc(@PathVariable UUID farmerId,
                                          @RequestBody @Valid KycVerificationRequest request) {
        kycService.verifyFarmer(farmerId, request.verifiedByAgentId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{farmerId}/kyc/reject")
    public ResponseEntity<Void> rejectKyc(@PathVariable UUID farmerId,
                                          @RequestBody @Valid KycVerificationRequest request) {
        kycService.rejectKyc(farmerId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{farmerId}/preferred-account/{accountId}")
    public ResponseEntity<Void> setPreferredAccount(@PathVariable UUID farmerId,
                                                    @PathVariable UUID accountId) {
        paymentService.setPreferredPayoutAccount(farmerId, accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{farmerId}/preferred-account")
    public ResponseEntity<PreferredPayoutAccount> getPreferredAccount(@PathVariable UUID farmerId) {
        return farmerDirectory.findPreferredPayoutAccount(farmerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}