package com.foodbridge.controller;

import com.foodbridge.dto.RouteView;
import com.foodbridge.entity.Donation;
import com.foodbridge.entity.User;
import com.foodbridge.service.DonationService;
import com.foodbridge.service.UserService;
import com.foodbridge.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final DonationService donationService;
    private final VolunteerService volunteerService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User volunteer = userService.findByEmail(principal.getName());
        List<Donation> available = donationService.getOpenDeliveryTasks();
        List<Donation> mine = donationService.getActiveTasksForVolunteer(volunteer);

        // source (hotel) -> destination (NGO) coordinates for the Leaflet map, keyed by donation id
        Map<Long, RouteView> routes = new LinkedHashMap<>();
        mine.forEach(d -> routes.put(d.getId(), toRoute(d)));
        available.forEach(d -> routes.put(d.getId(), toRoute(d)));

        model.addAttribute("volunteer", volunteer);
        model.addAttribute("stats", volunteerService.getStats(volunteer));
        model.addAttribute("availableTasks", available);
        model.addAttribute("myTasks", mine);
        model.addAttribute("routes", routes);
        model.addAttribute("certificates", volunteerService.getCertificates(volunteer));
        return "volunteer-dashboard";
    }

    private static RouteView toRoute(Donation d) {
        User hotel = d.getHotel();
        User ngo = d.getNgo();
        String srcAddress = d.getPickupAddress() != null && !d.getPickupAddress().isBlank() ? d.getPickupAddress() : hotel.getAddress();
        return new RouteView(d.getId(),
                hotel.getName(), srcAddress, hotel.getLatitude(), hotel.getLongitude(),
                ngo == null ? null : ngo.getName(), ngo == null ? null : ngo.getAddress(),
                ngo == null ? null : ngo.getLatitude(), ngo == null ? null : ngo.getLongitude());
    }

    @PostMapping("/accept/{id}")
    public String accept(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User volunteer = userService.findByEmail(principal.getName());
        donationService.acceptTask(id, volunteer);
        ra.addFlashAttribute("success", "Task accepted. Go to the hotel and ask for the pickup OTP.");
        return "redirect:/volunteer/dashboard";
    }

    @PostMapping("/verify-pickup")
    public String verifyPickup(@RequestParam Long donationId, @RequestParam String otp,
                               Principal principal, RedirectAttributes ra) {
        User volunteer = userService.findByEmail(principal.getName());
        donationService.verifyPickupOtp(donationId, otp, volunteer);
        ra.addFlashAttribute("success", "Pickup verified. Deliver the food to the NGO and ask for the delivery OTP.");
        return "redirect:/volunteer/dashboard";
    }

    @PostMapping("/verify-delivery")
    public String verifyDelivery(@RequestParam Long donationId, @RequestParam String otp,
                                 Principal principal, RedirectAttributes ra) {
        User volunteer = userService.findByEmail(principal.getName());
        donationService.verifyDeliveryOtp(donationId, otp, volunteer);
        ra.addFlashAttribute("success", "Delivery complete! +" + VolunteerService.POINTS_PER_DELIVERY + " points. Thank you!");
        return "redirect:/volunteer/dashboard";
    }

    @GetMapping("/certificate/download/{id}")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id, Principal principal) {
        User volunteer = userService.findByEmail(principal.getName());
        byte[] pdf = volunteerService.generateCertificatePdf(id, volunteer);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Spreading-Smiles-Certificate-" + id + ".pdf").build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
