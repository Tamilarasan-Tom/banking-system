package com.example.banking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE loans MODIFY COLUMN loan_type VARCHAR(50)");
            System.out.println("DatabaseSchemaFixer: Successfully updated loans.loan_type column definition to VARCHAR(50).");
        } catch (Exception e) {
            System.err.println("DatabaseSchemaFixer notice: " + e.getMessage());
        }
    }
}
