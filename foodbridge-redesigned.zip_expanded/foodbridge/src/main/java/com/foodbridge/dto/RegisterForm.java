package com.foodbridge.dto;

import com.foodbridge.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Choose a role")
    private Role role;

    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    /** Map position, set by the location picker for hotels and NGOs (optional). */
    @DecimalMin(value = "-90", message = "Invalid latitude")
    @DecimalMax(value = "90", message = "Invalid latitude")
    private Double latitude;

    @DecimalMin(value = "-180", message = "Invalid longitude")
    @DecimalMax(value = "180", message = "Invalid longitude")
    private Double longitude;
}
