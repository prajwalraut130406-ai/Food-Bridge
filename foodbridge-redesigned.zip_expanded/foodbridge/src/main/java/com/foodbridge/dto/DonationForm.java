package com.foodbridge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DonationForm {

    @Valid
    @NotEmpty(message = "Add at least one food item")
    @Size(max = 20, message = "At most 20 items per donation")
    private List<DonationItemForm> items = new ArrayList<>();

    /** Optional photo of the food (JPG, PNG, GIF or WebP, max 5 MB). */
    private MultipartFile foodImage;

    @NotNull(message = "Preparation time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime preparationTime;

    @NotNull(message = "Expiry (hours) is required")
    @Min(value = 1, message = "Minimum 1 hour")
    @Max(value = 72, message = "Maximum 72 hours")
    private Integer expiryHours;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @Size(max = 500)
    private String specialInstructions;
}
