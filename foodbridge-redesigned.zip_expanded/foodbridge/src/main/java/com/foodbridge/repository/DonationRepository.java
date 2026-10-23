package com.foodbridge.repository;

import com.foodbridge.entity.Donation;
import com.foodbridge.entity.DonationStatus;
import com.foodbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByStatus(DonationStatus status);
    List<Donation> findByHotel(User hotel);
    List<Donation> findByVolunteer(User volunteer);
    List<Donation> findByNgo(User ngo);

    List<Donation> findByStatusOrderByCreatedAtDesc(DonationStatus status);
    List<Donation> findByHotelOrderByCreatedAtDesc(User hotel);
    List<Donation> findByNgoOrderByCreatedAtDesc(User ngo);
    List<Donation> findByVolunteerOrderByCreatedAtDesc(User volunteer);

    /** Claimed by an NGO but no volunteer yet = open delivery tasks. */
    List<Donation> findByStatusAndVolunteerIsNullOrderByCreatedAtDesc(DonationStatus status);
}
