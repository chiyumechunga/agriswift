package zm.agriswift.identity.internal.service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FarmerRegisterRequest(
        @NotBlank(message = "NRC is required")
        @Pattern(regexp = "^[0-9]{6}/[0-9]{2}/[0-9]{1}$", message = "Invalid Zambian NRC format")
        String nrc,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Email is required for OTP delivery")
        @Email(message = "Invalid email address")
        String email,

        String province,
        String district,

        @NotBlank(message = "PIN is required")
        @Size(min = 4, max = 4, message = "PIN must be exactly 4 digits")
        String pin
) {}