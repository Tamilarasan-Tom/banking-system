package com.example.banking.service;

import com.example.banking.dto.TransactionRequest;
import com.example.banking.dto.TransferRequest;
import com.example.banking.entity.*;
import com.example.banking.exception.InsufficientBalanceException;
import com.example.banking.exception.InvalidTransactionException;
import com.example.banking.exception.ResourceNotFoundException;
import com.example.banking.exception.UnauthorizedException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            NotificationService notificationService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    private void verifyTransactionPin(User user, String pin) {

        if (user.getTransactionPin() == null ||
                user.getTransactionPin().isBlank()) {

            throw new InvalidTransactionException(
                    "Transaction PIN has not been created. Please create your PIN first.");
        }

        if (pin == null || pin.isBlank()) {
            throw new InvalidTransactionException(
                    "Transaction PIN is required.");
        }

        if (!passwordEncoder.matches(pin, user.getTransactionPin())) {
            throw new InvalidTransactionException(
                    "Invalid transaction PIN.");
        }
    }

    @Override
    public void createTransactionPin(User user, String pin) {

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

    @Override
    public Transaction deposit(User user, TransactionRequest request) {

        verifyTransactionPin(user, request.getPin());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with account number: " + request.getAccountNumber()));

        // Security Check: Normal user can only deposit to their own account
        if (user.getRole() == Role.ROLE_USER && !account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own this account.");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Transaction failed: Account is " + account.getStatus().name());
        }

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setPreviousBalance(oldBalance);
        transaction.setNewBalance(newBalance);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Self Deposit");

        Transaction savedTxn = transactionRepository.save(transaction);

        notificationService.createNotification(
                account.getUser(),
                "Deposit Successful",
                "An amount of $" + request.getAmount() + " has been deposited into your account "
                        + account.getAccountNumber() + ". New Balance: $" + newBalance,
                NotificationType.TRANSACTION);

        return savedTxn;
    }

    @Override
    public Transaction withdraw(User user, TransactionRequest request) {

        verifyTransactionPin(user, request.getPin());

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with account number: " + request.getAccountNumber()));

        // Security Check: Normal user can only withdraw from their own account
        if (user.getRole() == Role.ROLE_USER && !account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own this account.");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Transaction failed: Account is " + account.getStatus().name());
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account: Available Balance is $" + account.getBalance());
        }

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setAmount(request.getAmount());
        transaction.setPreviousBalance(oldBalance);
        transaction.setNewBalance(newBalance);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Self Withdrawal");

        Transaction savedTxn = transactionRepository.save(transaction);

        notificationService.createNotification(
                account.getUser(),
                "Withdrawal Successful",
                "An amount of $" + request.getAmount() + " has been withdrawn from your account "
                        + account.getAccountNumber() + ". New Balance: $" + newBalance,
                NotificationType.TRANSACTION);

        return savedTxn;
    }

    @Override
    public void transfer(User user, TransferRequest request) {
        verifyTransactionPin(user, request.getPin());
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        Account sender = accountRepository.findByAccountNumber(request.getSenderAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sender account not found: " + request.getSenderAccountNumber()));

        Account recipient = accountRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipient account not found: " + request.getRecipientAccountNumber()));

        // Business Rule validations
        if (sender.getAccountNumber().equals(recipient.getAccountNumber())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        // Security Check: Normal user can only transfer from their own account
        if (user.getRole() == Role.ROLE_USER && !sender.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own the sender account.");
        }

        if (sender.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Transaction failed: Sender account is " + sender.getStatus().name());
        }

        if (recipient.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(
                    "Transaction failed: Recipient account is " + recipient.getStatus().name());
        }

        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance: Sender account balance is $" + sender.getBalance());
        }

        // Update balances
        BigDecimal senderOldBal = sender.getBalance();
        BigDecimal senderNewBal = senderOldBal.subtract(request.getAmount());
        sender.setBalance(senderNewBal);
        accountRepository.save(sender);

        BigDecimal recipientOldBal = recipient.getBalance();
        BigDecimal recipientNewBal = recipientOldBal.add(request.getAmount());
        recipient.setBalance(recipientNewBal);
        accountRepository.save(recipient);

        // Record Sender Transaction
        Transaction senderTxn = new Transaction();
        senderTxn.setAccount(sender);
        senderTxn.setTransactionType(TransactionType.TRANSFER);
        senderTxn.setAmount(request.getAmount());
        senderTxn.setPreviousBalance(senderOldBal);
        senderTxn.setNewBalance(senderNewBal);
        senderTxn.setStatus(TransactionStatus.SUCCESS);
        senderTxn.setDescription("Transferred to " + recipient.getAccountNumber()
                + (request.getDescription() != null ? " - " + request.getDescription() : ""));
        transactionRepository.save(senderTxn);

        // Record Recipient Transaction
        Transaction recipientTxn = new Transaction();
        recipientTxn.setAccount(recipient);
        recipientTxn.setTransactionType(TransactionType.TRANSFER);
        recipientTxn.setAmount(request.getAmount());
        recipientTxn.setPreviousBalance(recipientOldBal);
        recipientTxn.setNewBalance(recipientNewBal);
        recipientTxn.setStatus(TransactionStatus.SUCCESS);
        recipientTxn.setDescription("Received from " + sender.getAccountNumber()
                + (request.getDescription() != null ? " - " + request.getDescription() : ""));
        transactionRepository.save(recipientTxn);

        // Create Notifications
        notificationService.createNotification(
                sender.getUser(),
                "Fund Transfer Out",
                "Your transfer of $" + request.getAmount() + " to account " + recipient.getAccountNumber()
                        + " was successful.",
                NotificationType.TRANSFER);

        notificationService.createNotification(
                recipient.getUser(),
                "Fund Transfer In",
                "You have received a transfer of $" + request.getAmount() + " from account " + sender.getAccountNumber()
                        + ".",
                NotificationType.TRANSFER);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(User user) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            return transactionRepository.findAllByOrderByTransactionDateDesc();
        }
        return transactionRepository.findByAccountUserIdOrderByTransactionDateDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistoryByAccount(Long accountId, User user) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Security check
        if (user.getRole() == Role.ROLE_USER && !account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Access Denied: You do not own this account.");
        }

        return transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByTransactionDateDesc();
    }

}
