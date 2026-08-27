package com.example.banking;

import com.example.banking.entity.*;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import com.example.banking.dto.AccountRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
public class BankingApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(BankingApplication.class, args);
    }

    private static void loadDotEnv() {
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                }
            } catch (java.io.IOException e) {
                System.err.println("Failed to load .env file: " + e.getMessage());
            }
        }
    }

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed/Reset Admin User
            User admin = userRepository.findByEmail("admin@bank.com").orElseGet(User::new);
            admin.setFullName("System Administrator");
            admin.setEmail("admin@bank.com");
            if (admin.getPhone() == null) admin.setPhone("1234567890");
            if (admin.getAddress() == null) admin.setAddress("Bank HQ, New York");
            if (admin.getDateOfBirth() == null) admin.setDateOfBirth(LocalDate.of(1985, 1, 1));
            admin.setPassword(passwordEncoder.encode("adminpassword"));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            System.out.println("Seeded/Updated admin user: admin@bank.com / adminpassword");

            // Seed/Reset Test User
            User user = userRepository.findByEmail("user@bank.com").orElseGet(User::new);
            boolean isNewUser = (user.getId() == null);
            user.setFullName("John Doe");
            user.setEmail("user@bank.com");
            if (user.getPhone() == null) user.setPhone("9876543210");
            if (user.getAddress() == null) user.setAddress("123 Elm Street, NY");
            if (user.getDateOfBirth() == null) user.setDateOfBirth(LocalDate.of(1990, 5, 15));
            user.setPassword(passwordEncoder.encode("userpassword"));
            user.setRole(Role.ROLE_USER);
            user.setStatus(UserStatus.ACTIVE);
            User savedUser = userRepository.save(user);
            System.out.println("Seeded/Updated customer user: user@bank.com / userpassword");

            if (isNewUser) {
                AccountRequest accountRequest = new AccountRequest();
                accountRequest.setAccountType("SAVINGS");
                accountRequest.setInitialBalance(new BigDecimal("1500.00"));
                Account account = accountService.createAccount(savedUser, accountRequest);
                System.out.println("Seeded Savings Account for John Doe: " + account.getAccountNumber() + " with balance $1500.00");
            }

            // Seed/Reset second test user
            User user2 = userRepository.findByEmail("jane@bank.com").orElseGet(User::new);
            boolean isNewUser2 = (user2.getId() == null);
            user2.setFullName("Jane Smith");
            user2.setEmail("jane@bank.com");
            if (user2.getPhone() == null) user2.setPhone("9998887776");
            if (user2.getAddress() == null) user2.setAddress("456 Oak Ave, California");
            if (user2.getDateOfBirth() == null) user2.setDateOfBirth(LocalDate.of(1995, 8, 20));
            user2.setPassword(passwordEncoder.encode("userpassword"));
            user2.setRole(Role.ROLE_USER);
            user2.setStatus(UserStatus.ACTIVE);
            User savedUser2 = userRepository.save(user2);
            System.out.println("Seeded/Updated customer user: jane@bank.com / userpassword");

            if (isNewUser2) {
                AccountRequest accountRequest = new AccountRequest();
                accountRequest.setAccountType("CHECKING");
                accountRequest.setInitialBalance(new BigDecimal("500.00"));
                Account account2 = accountService.createAccount(savedUser2, accountRequest);
                System.out.println("Seeded Checking Account for Jane Smith: " + account2.getAccountNumber() + " with balance $500.00");
            }
        };
    }
}
