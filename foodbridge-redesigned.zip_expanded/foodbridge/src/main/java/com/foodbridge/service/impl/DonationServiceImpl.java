package com.foodbridge.service.impl;

import com.foodbridge.dto.DonationForm;
import com.foodbridge.dto.DonationItemForm;
import com.foodbridge.entity.Donation;
import com.foodbridge.entity.DonationItem;
import com.foodbridge.entity.DonationStatus;
import com.foodbridge.entity.User;
import com.foodbridge.exception.BusinessException;
import com.foodbridge.repository.DonationItemRepository;
import com.foodbridge.repository.DonationRepository;
import com.foodbridge.service.DonationService;
import com.foodbridge.service.FileStorageService;
import com.foodbridge.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Status flow: AVAILABLE -> (NGO claims) CLAIMED -> (volunteer accepts + pickup OTP) IN_TRANSIT
 *              -> (delivery OTP) DELIVERED.
 * noRollbackFor: failed-OTP attempt counters must persist even though we throw.
 */
@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = BusinessException.class)
public class DonationServiceImpl implements DonationService {

    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DonationRepository donationRepository;
    private final VolunteerService volunteerService;
    private final DonationItemRepository donationItemRepository;
    private final FileStorageService fileStorageService;

    // ------------------------------------------------------------ Hotel

    @Override
    public Donation postDonation(DonationForm form, User hotel) {
        List<DonationItemForm> rows = form.getItems() == null ? List.of()
                : form.getItems().stream().filter(Objects::nonNull).toList();
        if (rows.isEmpty()) {
            throw new BusinessException("Add at least one food item.");
        }

        Donation d = new Donation();
        d.setHotel(hotel);
        d.setFoodTitle(summaryTitle(rows));
        d.setPreparationTime(form.getPreparationTime());
        d.setExpiryHours(form.getExpiryHours());
        d.setPickupAddress(form.getPickupAddress().trim());
        d.setSpecialInstructions(form.getSpecialInstructions());
        d.setStatus(DonationStatus.AVAILABLE);
        if (d.isExpired()) {
            throw new BusinessException("This food has already expired based on the preparation time and expiry hours.");
        }

        List<DonationItem> items = new ArrayList<>();
        double total = 0;
        for (DonationItemForm r : rows) {
            DonationItem item = new DonationItem();
            item.setItemName(r.getItemName().trim());
            item.setFoodType(r.getFoodType());
            item.setQuantity(r.getQuantity());
            item.setUnit(r.getUnit());
            item.setApproximatePrice(r.getApproximatePrice());
            items.add(item);
            total += r.getApproximatePrice();
        }
        d.setTotalApproximateValue(Math.round(total * 100.0) / 100.0);

        // Validates (type, size) and stores the photo; removed again if the database write fails.
        String imageUrl = fileStorageService.store(form.getFoodImage());
        d.setFoodImageUrl(imageUrl);
        try {
            Donation saved = donationRepository.save(d);
            items.forEach(i -> i.setDonation(saved));
            saved.getItems().addAll(donationItemRepository.saveAll(items)); // batch save of all items
            return saved;
        } catch (RuntimeException e) {
            fileStorageService.delete(imageUrl);
            throw e;
        }
    }

    private static String summaryTitle(List<DonationItemForm> rows) {
        String t = rows.stream().map(r -> r.getItemName().trim()).distinct().collect(java.util.stream.Collectors.joining(", "));
        return t.length() > 250 ? t.substring(0, 247) + "..." : t;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getDonationsByHotel(User hotel) {
        return donationRepository.findByHotelOrderByCreatedAtDesc(hotel);
    }

    @Override
    public Donation generatePickupOtp(Long donationId, User hotel) {
        Donation d = find(donationId);
        if (!sameUser(d.getHotel(), hotel)) {
            throw new BusinessException("This is not your donation.");
        }
        if (d.getStatus() != DonationStatus.CLAIMED) {
            throw new BusinessException("A pickup OTP can only be generated while the donation is claimed and awaiting pickup.");
        }
        d.setPickupOtp(newOtp());
        d.setPickupAttempts(0);
        return donationRepository.save(d);
    }

    // ------------------------------------------------------------ NGO

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getAvailableDonations() {
        return donationRepository.findByStatusOrderByCreatedAtDesc(DonationStatus.AVAILABLE).stream()
                .filter(d -> !d.isExpired())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getDonationsByNgo(User ngo) {
        return donationRepository.findByNgoOrderByCreatedAtDesc(ngo);
    }

    @Override
    public Donation claimDonation(Long donationId, User ngo) {
        Donation d = find(donationId);
        if (d.getStatus() != DonationStatus.AVAILABLE) {
            throw new BusinessException("Sorry, this donation was already claimed.");
        }
        if (d.isExpired()) {
            throw new BusinessException("This donation has expired.");
        }
        d.setNgo(ngo);
        d.setStatus(DonationStatus.CLAIMED);
        return saveGuarded(d, "Sorry, this donation was just claimed by someone else.");
    }

    @Override
    public Donation generateDeliveryOtp(Long donationId, User ngo) {
        Donation d = find(donationId);
        if (!sameUser(d.getNgo(), ngo)) {
            throw new BusinessException("This is not your claimed donation.");
        }
        if (d.getStatus() != DonationStatus.CLAIMED && d.getStatus() != DonationStatus.IN_TRANSIT) {
            throw new BusinessException("A delivery OTP can only be generated for an active delivery.");
        }
        d.setDeliveryOtp(newOtp());
        d.setDeliveryAttempts(0);
        return donationRepository.save(d);
    }

    // ------------------------------------------------------------ Volunteer

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getOpenDeliveryTasks() {
        return donationRepository.findByStatusAndVolunteerIsNullOrderByCreatedAtDesc(DonationStatus.CLAIMED).stream()
                .filter(d -> !d.isExpired())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getActiveTasksForVolunteer(User volunteer) {
        return donationRepository.findByVolunteerOrderByCreatedAtDesc(volunteer).stream()
                .filter(d -> d.getStatus() == DonationStatus.CLAIMED || d.getStatus() == DonationStatus.IN_TRANSIT)
                .toList();
    }

    @Override
    public Donation acceptTask(Long donationId, User volunteer) {
        Donation d = find(donationId);
        if (d.getStatus() != DonationStatus.CLAIMED || d.getVolunteer() != null) {
            throw new BusinessException("This task is no longer available.");
        }
        if (d.isExpired()) {
            throw new BusinessException("This donation has expired.");
        }
        d.setVolunteer(volunteer);
        return saveGuarded(d, "Sorry, another volunteer just accepted this task.");
    }

    @Override
    public Donation verifyPickupOtp(Long donationId, String otp, User volunteer) {
        Donation d = find(donationId);
        requireAssigned(d, volunteer);
        if (d.getStatus() != DonationStatus.CLAIMED) {
            throw new BusinessException("Pickup already verified or not applicable.");
        }
        if (d.getPickupOtp() == null) {
            throw new BusinessException("The hotel has not generated a pickup OTP yet. Ask them to generate one.");
        }
        if (d.getPickupAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new BusinessException("Too many wrong attempts. Ask the hotel to generate a new OTP.");
        }
        if (!otpMatches(d.getPickupOtp(), otp)) {
            d.setPickupAttempts(d.getPickupAttempts() + 1);
            donationRepository.save(d);
            throw new BusinessException("Incorrect pickup OTP.");
        }
        d.setPickupOtp(null); // one-time use
        d.setPickupAttempts(0);
        d.setStatus(DonationStatus.IN_TRANSIT);
        return donationRepository.save(d);
    }

    @Override
    public Donation verifyDeliveryOtp(Long donationId, String otp, User volunteer) {
        Donation d = find(donationId);
        requireAssigned(d, volunteer);
        if (d.getStatus() != DonationStatus.IN_TRANSIT) {
            throw new BusinessException("Verify the pickup first, or this delivery is already complete.");
        }
        if (d.getDeliveryOtp() == null) {
            throw new BusinessException("The NGO has not generated a delivery OTP yet. Ask them to generate one.");
        }
        if (d.getDeliveryAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new BusinessException("Too many wrong attempts. Ask the NGO to generate a new OTP.");
        }
        if (!otpMatches(d.getDeliveryOtp(), otp)) {
            d.setDeliveryAttempts(d.getDeliveryAttempts() + 1);
            donationRepository.save(d);
            throw new BusinessException("Incorrect delivery OTP.");
        }
        d.setDeliveryOtp(null);
        d.setDeliveryAttempts(0);
        d.setStatus(DonationStatus.DELIVERED);
        d.setDeliveredAt(LocalDateTime.now());
        Donation saved = donationRepository.save(d);

        volunteerService.recordDelivery(volunteer); // +100 points, rank promotion, certificate
        return saved;
    }

    // ------------------------------------------------------------ helpers

    private Donation find(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Donation not found."));
    }

    private void requireAssigned(Donation d, User volunteer) {
        if (d.getVolunteer() == null || !sameUser(d.getVolunteer(), volunteer)) {
            throw new BusinessException("This task is not assigned to you.");
        }
    }

    private boolean sameUser(User a, User b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    private Donation saveGuarded(Donation d, String conflictMessage) {
        try {
            return donationRepository.saveAndFlush(d);
        } catch (OptimisticLockingFailureException e) {
            throw new BusinessException(conflictMessage);
        }
    }

    private static String newOtp() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }

    private static boolean otpMatches(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.trim().getBytes(StandardCharsets.UTF_8));
    }
}
