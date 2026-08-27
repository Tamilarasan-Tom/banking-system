package com.example.banking.repository;

import com.example.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    List<Transaction> findByAccountUserIdOrderByTransactionDateDesc(Long userId);

    List<Transaction> findAllByOrderByTransactionDateDesc();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.transactionType = com.example.banking.entity.TransactionType.TRANSFER AND t.previousBalance > t.newBalance")
    java.math.BigDecimal sumTotalTransfers();
}
