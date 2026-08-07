package org.anamaria.booking.system.controller;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.ProviderRegisterRequest;
import org.anamaria.booking.system.dto.authentication.AuthResponse;
import org.anamaria.booking.system.service.ProviderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/provider")
@RequiredArgsConstructor
public class ProviderAuthController {

    private final ProviderService providerService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody ProviderRegisterRequest request) {
        return providerService.registerProvider(request);
    }
}