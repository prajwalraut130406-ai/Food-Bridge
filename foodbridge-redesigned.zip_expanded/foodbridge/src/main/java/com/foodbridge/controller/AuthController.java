package com.foodbridge.controller;

import com.foodbridge.dto.RegisterForm;
import com.foodbridge.entity.Role;
import com.foodbridge.exception.BusinessException;
import com.foodbridge.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    public static String dashboardFor(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority a : authorities) {
            if (Role.ROLE_HOTEL.name().equals(a.getAuthority())) return "/hotel/dashboard";
            if (Role.ROLE_NGO.name().equals(a.getAuthority())) return "/ngo/dashboard";
            if (Role.ROLE_VOLUNTEER.name().equals(a.getAuthority())) return "/volunteer/dashboard";
        }
        return "/login";
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }
        return "redirect:" + dashboardFor(authentication.getAuthorities());
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("roles", Role.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result, Model model, RedirectAttributes ra) {
        model.addAttribute("roles", Role.values());
        if (form.getEmail() != null && userService.emailExists(form.getEmail())) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form);
        } catch (BusinessException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        }
        ra.addFlashAttribute("success", "Registration successful. Please log in.");
        return "redirect:/login";
    }
}
