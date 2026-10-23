package com.foodbridge.repository;

import com.foodbridge.entity.Certificate;
import com.foodbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByVolunteerOrderByIssuedDateDescIdDesc(User volunteer);
}
