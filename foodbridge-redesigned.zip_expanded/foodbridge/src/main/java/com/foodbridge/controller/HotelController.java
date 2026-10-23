package com.foodbridge.controller;

import com.foodbridge.dto.DonationForm;
import com.foodbridge.dto.DonationItemForm;
import com.foodbridge.entity.FoodType;
import com.foodbridge.entity.FoodUnit;
import com.foodbridge.entity.User;
import com.foodbridge.exception.BusinessException;
import com.foodbridge.service.DonationService;
import com.foodbridge.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/hotel")
@RequiredArgsConstructor
public class HotelController {

    /** Suggestions for the "menu item" dropdown (the hotel can also type any other dish). */
    private static final List<String> MENU_ITEMS = List.of(
            "Steamed rice", "Jeera rice", "Veg pulao", "Veg biryani", "Chicken biryani",
            "Dal tadka", "Dal makhani", "Roti / Chapati", "Naan", "Paratha",
            "Paneer butter masala", "Mixed veg curry", "Chole", "Rajma", "Sambar",
            "Idli", "Dosa", "Poha", "Upma", "Sandwiches", "Salad", "Soup",
            "Chicken curry", "Egg curry", "Fish curry", "Mutton curry",
            "Gulab jamun", "Kheer", "Milk", "Fruit juice", "Buttermilk");

    private final DonationService donationService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        User hotel = userService.findByEmail(principal.getName());
        if (!model.containsAttribute("donationForm")) {
            DonationForm form = new DonationForm();
            form.setPickupAddress(hotel.getAddress());
            form.setPreparationTime(LocalDateTime.now().withSecond(0).withNano(0));
            form.getItems().add(new DonationItemForm());
            model.addAttribute("donationForm", form);
        }
        return view(hotel, model);
    }

    @PostMapping("/donate")
    public String donate(@Valid @ModelAttribute("donationForm") DonationForm form, BindingResult result,
                         Principal principal, Model model, RedirectAttributes ra) {
        User hotel = userService.findByEmail(principal.getName());
        if (!result.hasErrors()) {
            try {
                donationService.postDonation(form, hotel);
                ra.addFlashAttribute("success", "Donation posted. NGOs can now claim it.");
                return "redirect:/hotel/dashboard";
            } catch (BusinessException e) {
                result.reject("business", e.getMessage());
            }
        }
        if (form.getItems() == null || form.getItems().isEmpty()) {
            form.setItems(new ArrayList<>(List.of(new DonationItemForm())));
        }
        return view(hotel, model);
    }

    @PostMapping("/otp/{id}")
    public String generatePickupOtp(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User hotel = userService.findByEmail(principal.getName());
        donationService.generatePickupOtp(id, hotel);
        ra.addFlashAttribute("success", "Pickup OTP generated. Share it with the volunteer only when they arrive.");
        return "redirect:/hotel/dashboard";
    }

    private String view(User hotel, Model model) {
        model.addAttribute("hotel", hotel);
        model.addAttribute("donations", donationService.getDonationsByHotel(hotel));
        model.addAttribute("menuItems", MENU_ITEMS);
        model.addAttribute("foodTypes", FoodType.values());
        model.addAttribute("foodUnits", FoodUnit.values());
        return "hotel-dashboard";
    }
}
