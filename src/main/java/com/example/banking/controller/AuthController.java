
package com.example.banking.controller;

import com.example.banking.dto.AuthResponse;
import com.example.banking.dto.LoginRequest;
import com.example.banking.dto.RegisterRequest;
import com.example.banking.entity.Role;
import com.example.banking.entity.User;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtTokenProvider;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtTokenProvider tokenProvider,
            UserRepository userRepository) {

        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest) {

        User user = userService.registerUser(registerRequest);

        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        AuthResponse authResponse = new AuthResponse(
                jwt,
                userDetails.getUsername(),
                userDetails.getUser().getFullName(),
                userDetails.getUser().getRole().name());

        return ResponseEntity.ok(authResponse);
    }

    /*
     * TEMPORARY ENDPOINT
     * Use this only to create the first admin account.
     * Remove this method after creating the admin.
     */
    @PostMapping("/create-admin")
    public ResponseEntity<User> createAdmin(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(request);

        user.setRole(Role.ROLE_ADMIN);

        userRepository.save(user);

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logoutUser() {

        SecurityContextHolder.clearContext();

        Map<String, String> response = new HashMap<>();

        response.put(
                "message",
                "Logged out successfully. Please clear the token from localStorage.");

        return ResponseEntity.ok(response);
    }
}