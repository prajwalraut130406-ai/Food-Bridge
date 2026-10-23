package com.foodbridge.service.impl;

import com.foodbridge.dto.VolunteerStats;
import com.foodbridge.entity.Certificate;
import com.foodbridge.entity.User;
import com.foodbridge.entity.VolunteerProfile;
import com.foodbridge.entity.VolunteerRank;
import com.foodbridge.exception.BusinessException;
import com.foodbridge.repository.CertificateRepository;
import com.foodbridge.repository.VolunteerProfileRepository;
import com.foodbridge.service.VolunteerService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class VolunteerServiceImpl implements VolunteerService {

    private static final String CERT_NAME = "Spreading Smiles Certificate";

    private final VolunteerProfileRepository profileRepository;
    private final CertificateRepository certificateRepository;

    @Override
    public VolunteerProfile getOrCreateProfile(User volunteer) {
        return profileRepository.findByUser(volunteer).orElseGet(() -> {
            VolunteerProfile p = new VolunteerProfile();
            p.setUser(volunteer);
            return profileRepository.save(p);
        });
    }

    /** +100 points, auto rank promotion, and a certificate on milestones. */
    @Override
    public void recordDelivery(User volunteer) {
        VolunteerProfile p = getOrCreateProfile(volunteer);
        VolunteerRank before = p.getRank();

        p.setPoints(p.getPoints() + POINTS_PER_DELIVERY);
        p.setTotalDeliveries(p.getTotalDeliveries() + 1);
        VolunteerRank after = VolunteerRank.fromPoints(p.getPoints());
        p.setRank(after);
        profileRepository.save(p);

        // Milestones: rank promotion, first delivery, every 5th delivery.
        String title = null;
        if (after != before) {
            title = CERT_NAME + " - Promoted to " + after.getLabel();
        } else if (p.getTotalDeliveries() == 1) {
            title = CERT_NAME + " - First Delivery";
        } else if (p.getTotalDeliveries() % 5 == 0) {
            title = CERT_NAME + " - " + p.getTotalDeliveries() + " Deliveries";
        }
        if (title != null) {
            Certificate c = new Certificate();
            c.setVolunteer(volunteer);
            c.setTitle(title);
            c.setIssuedDate(LocalDate.now());
            certificateRepository.save(c);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VolunteerStats getStats(User volunteer) {
        VolunteerProfile p = profileRepository.findByUser(volunteer).orElse(null);
        int points = p == null ? 0 : p.getPoints();
        int deliveries = p == null ? 0 : p.getTotalDeliveries();
        VolunteerRank rank = VolunteerRank.fromPoints(points);
        VolunteerRank next = rank.next();

        int percent = 100;
        int toNext = 0;
        if (next != null) {
            int span = next.getMinPoints() - rank.getMinPoints();
            percent = (int) Math.max(0, Math.min(100, Math.round(100.0 * (points - rank.getMinPoints()) / span)));
            toNext = next.getMinPoints() - points;
        }
        return new VolunteerStats(points, deliveries, rank, next, percent, toNext);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certificate> getCertificates(User volunteer) {
        return certificateRepository.findByVolunteerOrderByIssuedDateDescIdDesc(volunteer);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(Long certificateId, User volunteer) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new BusinessException("Certificate not found."));
        if (!Objects.equals(cert.getVolunteer().getId(), volunteer.getId())) {
            throw new BusinessException("This certificate does not belong to you.");
        }
        VolunteerRank rank = profileRepository.findByUser(volunteer)
                .map(VolunteerProfile::getRank).orElse(VolunteerRank.BRONZE);

        Rectangle page = PageSize.A4.rotate();
        Document doc = new Document(page, 60, 60, 60, 60);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            // Decorative double border
            Color green = new Color(25, 135, 84);
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(green);
            cb.setLineWidth(5f);
            cb.rectangle(25, 25, page.getWidth() - 50, page.getHeight() - 50);
            cb.stroke();
            cb.setLineWidth(1.5f);
            cb.rectangle(35, 35, page.getWidth() - 70, page.getHeight() - 70);
            cb.stroke();

            doc.add(centered(CERT_NAME, new Font(Font.HELVETICA, 36, Font.BOLD, green), 30, 20));
            doc.add(centered("This certificate is proudly presented to",
                    new Font(Font.HELVETICA, 16, Font.NORMAL, Color.DARK_GRAY), 0, 15));
            doc.add(centered(volunteer.getName(),
                    new Font(Font.TIMES_ROMAN, 40, Font.BOLDITALIC, Color.BLACK), 0, 15));
            doc.add(centered("in recognition of: " + cert.getTitle().replace(CERT_NAME + " - ", ""),
                    new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY), 0, 10));
            doc.add(centered("for rescuing surplus food and helping bridge it to people in need.",
                    new Font(Font.HELVETICA, 14, Font.NORMAL, Color.DARK_GRAY), 0, 10));
            doc.add(centered("Current tier: " + rank.getLabel(),
                    new Font(Font.HELVETICA, 14, Font.ITALIC, green), 0, 30));
            doc.add(centered("Issued on " + cert.getIssuedDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                            + "  |  FoodBridge",
                    new Font(Font.HELVETICA, 12, Font.NORMAL, Color.GRAY), 0, 0));

            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate certificate PDF", e);
        }
        return out.toByteArray();
    }

    private static Paragraph centered(String text, Font font, float spaceBefore, float spaceAfter) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(spaceBefore);
        p.setSpacingAfter(spaceAfter);
        return p;
    }
}
