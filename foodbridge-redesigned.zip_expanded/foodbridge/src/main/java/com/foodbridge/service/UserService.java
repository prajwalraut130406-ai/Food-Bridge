package com.foodbridge.service;

import com.foodbridge.dto.RegisterForm;
import com.foodbridge.entity.User;

public interface UserService {
    User register(RegisterForm form);
    User findByEmail(String email);
    boolean emailExists(String email);
}
