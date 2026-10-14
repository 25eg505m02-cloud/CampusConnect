package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.AuthResponse;
import edu.campusconnect.dto.ApiDtos.RegisterRequest;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.UserRepository;
import edu.campusconnect.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       UserService userService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.badRequest("Password must not exceed 72 UTF-8 bytes.");
        }
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        users.saveAndFlush(user);
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password));
        } catch (AuthenticationException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        User user = users.findByEmailIgnoreCase(email.trim())
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        return userService.toResponse(userService.findActiveUser(email));
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", userService.toResponse(user));
    }
}
