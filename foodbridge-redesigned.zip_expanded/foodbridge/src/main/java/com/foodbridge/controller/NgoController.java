package com.foodbridge.controller;

import com.foodbridge.entity.Donation;
import com.foodbridge.entity.User;
import com.foodbridge.service.DonationService;
import com.foodbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/ngo")
@RequiredArgsConstructor
public class NgoController {

    private final DonationService donationService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User ngo = userService.findByEmail(principal.getName());
        model.addAttribute("ngo", ngo);
        List<Donation> available = donationService.getAvailableDonations();
        model.addAttribute("available", available);
        // approximate value (rupees) of everything currently on offer
        model.addAttribute("availableValue", available.stream().mapToDouble(Donation::getTotalApproximateValue).sum());
        model.addAttribute("myClaims", donationService.getDonationsByNgo(ngo));
        return "ngo-dashboard";
    }

    @PostMapping("/claim/{id}")
    public String claim(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User ngo = userService.findByEmail(principal.getName());
        donationService.claimDonation(id, ngo);
        ra.addFlashAttribute("success", "Donation claimed. A volunteer can now pick it up.");
        return "redirect:/ngo/dashboard";
    }

    @PostMapping("/otp/{id}")
    public String generateDeliveryOtp(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User ngo = userService.findByEmail(principal.getName());
        donationService.generateDeliveryOtp(id, ngo);
        ra.addFlashAttribute("success", "Delivery OTP generated. Give it to the volunteer when the food arrives.");
        return "redirect:/ngo/dashboard";
    }
}
