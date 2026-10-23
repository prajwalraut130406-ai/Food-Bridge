package com.foodbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One line of a donation (e.g. "Dal tadka, Veg, 5 liter, approx. 400"). */
@Entity
@Table(name = "donation_items")
@Getter
@Setter
@NoArgsConstructor
public class DonationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donation_id")
    private Donation donation;

    @Column(nullable = false, length = 150)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodType foodType;

    @Column(nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodUnit unit;

    /** Estimated monetary value of this line, in rupees. */
    @Column(nullable = false)
    private Double approximatePrice = 0.0;

    /** 5.0 -> "5", 2.5 -> "2.5" (for display only). */
    public String getDisplayQuantity() {
        if (quantity == null) {
            return "";
        }
        return BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString();
    }
}
