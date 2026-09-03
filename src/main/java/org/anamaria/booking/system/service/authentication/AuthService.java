package org.anamaria.booking.system.service.authentication;

import lombok.RequiredArgsConstructor;
import org.anamaria.booking.system.dto.authentication.AuthResponse;
import org.anamaria.booking.system.dto.authentication.LoginRequest;
import org.anamaria.booking.system.dto.authentication.RegisterRequest;
import org.anamaria.booking.system.exception.EmailAlreadyExistsException;
import org.anamaria.booking.system.exception.InvalidCredentialsException;
import org.anamaria.booking.system.exception.UserNotFoundException;
import org.anamaria.booking.system.exception.UsernameAlreadyExistsException;
import org.anamaria.booking.system.model.User;
import org.anamaria.booking.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User.Role role = (request.role() == null || request.role().isBlank())
                ? User.Role.CLIENT
                : User.Role.valueOf(request.role());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        return new AuthResponse(jwtService.generateToken(user));
    }
}
