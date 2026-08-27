package com.example.banking.service;

import com.example.banking.dto.RegisterRequest;
import com.example.banking.dto.UserProfileUpdate;
import com.example.banking.entity.User;
import com.example.banking.entity.UserStatus;
import java.util.List;

public interface UserService {
    User registerUser(RegisterRequest request);

    User updateUserProfile(User user, UserProfileUpdate update);

    User getUserById(Long id);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    void updateUserStatus(Long id, UserStatus status);

    void setTransactionPin(User user, String pin);
}
