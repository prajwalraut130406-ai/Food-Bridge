package com.foodbridge.service;

import com.foodbridge.dto.DonationForm;
import com.foodbridge.entity.Donation;
import com.foodbridge.entity.User;

import java.util.List;

public interface DonationService {

    // Hotel
    Donation postDonation(DonationForm form, User hotel);
    List<Donation> getDonationsByHotel(User hotel);
    Donation generatePickupOtp(Long donationId, User hotel);

    // NGO
    List<Donation> getAvailableDonations();
    List<Donation> getDonationsByNgo(User ngo);
    Donation claimDonation(Long donationId, User ngo);
    Donation generateDeliveryOtp(Long donationId, User ngo);

    // Volunteer
    List<Donation> getOpenDeliveryTasks();
    List<Donation> getActiveTasksForVolunteer(User volunteer);
    Donation acceptTask(Long donationId, User volunteer);
    Donation verifyPickupOtp(Long donationId, String otp, User volunteer);
    Donation verifyDeliveryOtp(Long donationId, String otp, User volunteer);
}
