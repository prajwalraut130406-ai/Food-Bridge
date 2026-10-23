package com.foodbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "volunteer_profiles")
@Getter
@Setter
@NoArgsConstructor
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private int points = 0;

    // "rank" is a reserved word in MySQL 8, so the column is renamed.
    @Enumerated(EnumType.STRING)
    @Column(name = "volunteer_rank", nullable = false, length = 20)
    private VolunteerRank rank = VolunteerRank.BRONZE;

    private int totalDeliveries = 0;
}
