package com.foodbridge.service.impl;

import com.foodbridge.dto.RegisterForm;
import com.foodbridge.entity.Role;
import com.foodbridge.entity.User;
import com.foodbridge.exception.BusinessException;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.UserService;
import com.foodbridge.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VolunteerService volunteerService;

    @Override
    public User register(RegisterForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("An account with this email already exists.");
        }
        User user = new User();
        user.setName(form.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(form.getRole());
        user.setPhone(form.getPhone());
        user.setAddress(form.getAddress());
        if (user.getRole() != Role.ROLE_VOLUNTEER) {
            user.setLatitude(form.getLatitude());
            user.setLongitude(form.getLongitude());
        }
        user = userRepository.save(user);

        if (user.getRole() == Role.ROLE_VOLUNTEER) {
            volunteerService.getOrCreateProfile(user);
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BusinessException("User not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }
}
