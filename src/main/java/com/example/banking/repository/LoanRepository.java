package com.example.banking.repository;

import com.example.banking.entity.Loan;
import com.example.banking.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserIdOrderByApplicationDateDesc(Long userId);
    List<Loan> findByLoanStatus(LoanStatus loanStatus);
    List<Loan> findAllByOrderByApplicationDateDesc();
    long countByLoanStatus(LoanStatus loanStatus);
}
