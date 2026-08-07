package org.anamaria.booking.system.service;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.ProviderRegisterRequest;
import org.anamaria.booking.system.dto.authentication.AuthResponse;
import org.anamaria.booking.system.exception.EmailAlreadyExistsException;
import org.anamaria.booking.system.exception.UsernameAlreadyExistsException;
import org.anamaria.booking.system.model.Provider;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.repository.ProviderRepository;
import org.anamaria.booking.system.repository.UserRepository;
import org.anamaria.booking.system.service.authentication.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse registerProvider(ProviderRegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.PROVIDER)
                .build();

        userRepository.save(user);

        Provider provider = Provider.builder()
                .user(user)
                .specialization(request.specialization())
                .avgAppointmentDuration(request.avgAppointmentDuration())
                .build();

        providerRepository.save(provider);
        return new AuthResponse(jwtService.generateToken(user));
    }
}
