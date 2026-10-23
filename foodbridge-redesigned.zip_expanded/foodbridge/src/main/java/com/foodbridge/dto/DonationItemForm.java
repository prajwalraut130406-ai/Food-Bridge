package com.foodbridge.dto;

import com.foodbridge.entity.FoodType;
import com.foodbridge.entity.FoodUnit;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationItemForm {

    @NotBlank(message = "Menu item is required")
    @Size(max = 150, message = "Item name is too long")
    private String itemName;

    @NotNull(message = "Choose Veg or Non-Veg")
    private FoodType foodType = FoodType.VEG;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.1", message = "Quantity must be at least 0.1")
    @DecimalMax(value = "100000", message = "Quantity is too large")
    private Double quantity;

    @NotNull(message = "Choose a unit")
    private FoodUnit unit = FoodUnit.KG;

    @NotNull(message = "Approximate value is required")
    @DecimalMin(value = "0", message = "Value cannot be negative")
    @DecimalMax(value = "100000000", message = "Value is too large")
    private Double approximatePrice;
}
