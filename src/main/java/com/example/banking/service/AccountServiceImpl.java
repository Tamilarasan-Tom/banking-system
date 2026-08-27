package com.example.banking.service;

import com.example.banking.dto.AccountRequest;
import com.example.banking.entity.*;
import com.example.banking.exception.InvalidTransactionException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.exception.UnauthorizedException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               NotificationService notificationService,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account createAccount(User user, AccountRequest request) {
        // Process Transaction PIN setup or verification
        if (request.getPin() != null && !request.getPin().isBlank()) {
            String pin = request.getPin().trim();
            if (!pin.matches("\\d{4}")) {
                throw new InvalidTransactionException("Transaction PIN must be exactly 4 digits.");
            }

            if (user.getTransactionPin() == null || user.getTransactionPin().isBlank()) {
                user.setTransactionPin(passwordEncoder.encode(pin));
                userRepository.save(user);
            } else {
                if (!passwordEncoder.matches(pin, user.getTransactionPin())) {
                    throw new InvalidTransactionException("Invalid Transaction PIN.");
                }
            }
        } else if (user.getTransactionPin() != null && !user.getTransactionPin().isBlank()) {
            throw new InvalidTransactionException("Transaction PIN is required.");
        }

        String finalAccountNumber;
        if (request.getAccountNumber() != null && !request.getAccountNumber().isBlank()) {
            String customAcct = request.getAccountNumber().trim();
            if (!customAcct.matches("\\d{10}")) {
                throw new InvalidTransactionException("Account Number must be exactly 10 digits.");
            }
            if (accountRepository.findByAccountNumber(customAcct).isPresent()) {
                throw new InvalidTransactionException("Account number already exists: " + customAcct);
            }
            finalAccountNumber = customAcct;
        } else {
            finalAccountNumber = generateAccountNumber();
        }

        Account account = new Account();
        account.setUser(user);
        account.setAccountType(AccountType.valueOf(request.getAccountType().toUpperCase()));
        account.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber(finalAccountNumber);

        Account savedAccount = accountRepository.save(account);

        // If there's an initial balance, log it as a deposit transaction
        if (savedAccount.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            Transaction txn = new Transaction();
            txn.setAccount(savedAccount);
            txn.setTransactionType(TransactionType.DEPOSIT);
            txn.setAmount(savedAccount.getBalance());
            txn.setPreviousBalance(BigDecimal.ZERO);
            txn.setNewBalance(savedAccount.getBalance());
            txn.setStatus(TransactionStatus.SUCCESS);
            txn.setDescription("Initial Deposit upon account activation");
            transactionRepository.save(txn);
        }

        notificationService.createNotification(
                user,
                "Bank Account Created",
                "Your " + savedAccount.getAccountType().name() + " account " + savedAccount.getAccountNumber() + " has been successfully created with balance: $" + savedAccount.getBalance(),
                NotificationType.TRANSACTION
        );

        return savedAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountById(Long id, User user) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Security check: normal user can only access their own account
        if (user.getRole() == Role.ROLE_USER && !account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own this account.");
        }

        return account;
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUser(User user) {
        return accountRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Override
    public void updateAccountStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        account.setStatus(status);
        accountRepository.save(account);

        notificationService.createNotification(
                account.getUser(),
                "Account Status Update",
                "Your account " + account.getAccountNumber() + " status has been set to: " + status.name(),
                NotificationType.SYSTEM
        );
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String number;
        do {
            long code = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            number = String.valueOf(code);
        } while (accountRepository.findByAccountNumber(number).isPresent());
        return number;
    }
}
