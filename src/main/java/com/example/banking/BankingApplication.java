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
        SpringApplication.run(BankingApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Admin User if not exists
            if (userRepository.findByEmail("admin@bank.com").isEmpty()) {
                User admin = new User();
                admin.setFullName("System Administrator");
                admin.setEmail("admin@bank.com");
                admin.setPhone("1234567890");
                admin.setAddress("Bank HQ, New York");
                admin.setDateOfBirth(LocalDate.of(1985, 1, 1));
                admin.setPassword(passwordEncoder.encode("adminpassword"));
                admin.setRole(Role.ROLE_ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                userRepository.save(admin);
                System.out.println("Seeded admin user: admin@bank.com / adminpassword");
            }

            // Seed Test User if not exists
            if (userRepository.findByEmail("user@bank.com").isEmpty()) {
                User user = new User();
                user.setFullName("John Doe");
                user.setEmail("user@bank.com");
                user.setPhone("9876543210");
                user.setAddress("123 Elm Street, NY");
                user.setDateOfBirth(LocalDate.of(1990, 5, 15));
                user.setPassword(passwordEncoder.encode("userpassword"));
                user.setRole(Role.ROLE_USER);
                user.setStatus(UserStatus.ACTIVE);
                User savedUser = userRepository.save(user);
                System.out.println("Seeded customer user: user@bank.com / userpassword");

                // Seed a bank account for the customer
                AccountRequest accountRequest = new AccountRequest();
                accountRequest.setAccountType("SAVINGS");
                accountRequest.setInitialBalance(new BigDecimal("1500.00"));
                Account account = accountService.createAccount(savedUser, accountRequest);
                System.out.println("Seeded Savings Account for John Doe: " + account.getAccountNumber() + " with balance $1500.00");
            }

            // Seed a second test user to facilitate transfer testing
            if (userRepository.findByEmail("jane@bank.com").isEmpty()) {
                User user2 = new User();
                user2.setFullName("Jane Smith");
                user2.setEmail("jane@bank.com");
                user2.setPhone("9998887776");
                user2.setAddress("456 Oak Ave, California");
                user2.setDateOfBirth(LocalDate.of(1995, 8, 20));
                user2.setPassword(passwordEncoder.encode("userpassword"));
                user2.setRole(Role.ROLE_USER);
                user2.setStatus(UserStatus.ACTIVE);
                User savedUser2 = userRepository.save(user2);
                System.out.println("Seeded customer user: jane@bank.com / userpassword");

                AccountRequest accountRequest = new AccountRequest();
                accountRequest.setAccountType("CHECKING");
                accountRequest.setInitialBalance(new BigDecimal("500.00"));
                Account account2 = accountService.createAccount(savedUser2, accountRequest);
                System.out.println("Seeded Checking Account for Jane Smith: " + account2.getAccountNumber() + " with balance $500.00");
            }
        };
    }
}
