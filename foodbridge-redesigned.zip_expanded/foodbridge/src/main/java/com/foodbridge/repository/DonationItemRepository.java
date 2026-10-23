package com.foodbridge.repository;

import com.foodbridge.entity.DonationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationItemRepository extends JpaRepository<DonationItem, Long> {
    List<DonationItem> findByDonationIdOrderByIdAsc(Long donationId);
}
