package com.example.banking.controller;

import com.example.banking.dto.TransactionPinRequest;
import com.example.banking.dto.UserProfileUpdate;
import com.example.banking.entity.User;
import com.example.banking.security.UserDetailsImpl;
import com.example.banking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getUserProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        User user = userService.getUserById(userDetails.getId());

        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateUserProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UserProfileUpdate profileUpdate) {

        User user = userService.updateUserProfile(
                userDetails.getUser(),
                profileUpdate);

        return ResponseEntity.ok(user);
    }

    @PutMapping("/transaction-pin")
    public ResponseEntity<String> setTransactionPin(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody TransactionPinRequest request) {

        userService.setTransactionPin(
                userDetails.getUser(),
                request.getPin());

        return ResponseEntity.ok(
                "Transaction PIN created successfully");
    }
}