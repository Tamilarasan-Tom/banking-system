package com.example.banking.service;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserProfileUpdate;
import com.example.banking.entity.NotificationType;
import com.example.banking.entity.Role;
import com.example.banking.entity.User;
import com.example.banking.entity.UserStatus;
import com.example.banking.exception.DuplicateResourceException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.banking.exception.InvalidTransactionException;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        // Send registration alert
        notificationService.createNotification(
                savedUser,
                "Welcome to Antigravity Bank",
                "Your user registration was successful. Welcome aboard, " + savedUser.getFullName() + "!",
                NotificationType.SYSTEM);

        return savedUser;
    }

    @Override
    public User updateUserProfile(User user, UserProfileUpdate update) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user.getId()));

        existingUser.setFullName(update.getFullName());
        existingUser.setPhone(update.getPhone());
        existingUser.setAddress(update.getAddress());
        existingUser.setDateOfBirth(update.getDateOfBirth());

        User updatedUser = userRepository.save(existingUser);

        notificationService.createNotification(
                updatedUser,
                "Profile Updated",
                "Your profile information was updated successfully.",
                NotificationType.SYSTEM);

        return updatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        userRepository.save(user);

        notificationService.createNotification(
                user,
                "Account Status Update",
                "Your login profile status has been set to: " + status.name(),
                NotificationType.SYSTEM);
    }

    @Override
    public void setTransactionPin(User user, String pin) {

        if (pin == null || !pin.matches("\\d{4}")) {
            throw new InvalidTransactionException(
                    "Transaction PIN must be exactly 4 digits.");
        }

        if (user.getTransactionPin() != null &&
                !user.getTransactionPin().isBlank()) {
            throw new InvalidTransactionException(
                    "Transaction PIN already exists.");
        }

        user.setTransactionPin(passwordEncoder.encode(pin));

        userRepository.save(user);
    }

}
