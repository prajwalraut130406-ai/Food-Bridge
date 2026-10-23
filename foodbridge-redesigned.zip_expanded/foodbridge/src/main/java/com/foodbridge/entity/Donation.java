package com.foodbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hotel_id")
    private User hotel;

    @ManyToOne
    @JoinColumn(name = "ngo_id")
    private User ngo;

    @ManyToOne
    @JoinColumn(name = "volunteer_id")
    private User volunteer;

    /** Short summary built from the item names, e.g. "Rice, Dal tadka, Chicken curry". */
    @Column(nullable = false)
    private String foodTitle;

    /** Public URL of the uploaded photo (served from /uploads/**), or null. */
    @Column(length = 300)
    private String foodImageUrl;

    /** Sum of the approximate value (rupees) of all items. */
    @Column(nullable = false)
    private Double totalApproximateValue = 0.0;

    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id ASC")
    private List<DonationItem> items = new ArrayList<>();

    private LocalDateTime preparationTime;

    @Column(nullable = false)
    private Integer expiryHours;

    @Column(length = 500)
    private String pickupAddress;

    @Column(length = 500)
    private String specialInstructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status = DonationStatus.AVAILABLE;

    @Column(length = 4)
    private String pickupOtp;

    @Column(length = 4)
    private String deliveryOtp;

    private int pickupAttempts;
    private int deliveryAttempts;

    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    /** Optimistic lock: stops two NGOs / volunteers grabbing the same donation. */
    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public LocalDateTime getExpiresAt() {
        LocalDateTime base = preparationTime != null ? preparationTime : createdAt;
        return base == null ? null : base.plusHours(expiryHours);
    }

    public boolean isExpired() {
        LocalDateTime exp = getExpiresAt();
        return exp != null && LocalDateTime.now().isAfter(exp);
    }
}
