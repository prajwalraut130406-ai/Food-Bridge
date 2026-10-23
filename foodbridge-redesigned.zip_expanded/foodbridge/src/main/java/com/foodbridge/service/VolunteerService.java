package com.foodbridge.service;

import com.foodbridge.dto.VolunteerStats;
import com.foodbridge.entity.Certificate;
import com.foodbridge.entity.User;
import com.foodbridge.entity.VolunteerProfile;

import java.util.List;

public interface VolunteerService {
    int POINTS_PER_DELIVERY = 100;

    VolunteerProfile getOrCreateProfile(User volunteer);
    void recordDelivery(User volunteer);
    VolunteerStats getStats(User volunteer);
    List<Certificate> getCertificates(User volunteer);
    byte[] generateCertificatePdf(Long certificateId, User volunteer);
}
