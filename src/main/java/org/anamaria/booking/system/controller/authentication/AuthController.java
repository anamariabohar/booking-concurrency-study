package org.anamaria.booking.system.controller.authentication;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.authentication.AuthResponse;
import org.anamaria.booking.system.dto.authentication.LoginRequest;
import org.anamaria.booking.system.dto.authentication.RegisterRequest;
import org.anamaria.booking.system.service.authentication.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
