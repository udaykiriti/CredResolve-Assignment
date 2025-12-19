package com.expenseshare.config;

import com.expenseshare.model.*;
import com.expenseshare.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Initialize sample data for demonstration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Data already exists
        }

        log.info("Initializing sample data...");

        // Create sample users
        User john = userRepository.save(User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password")
                .build());

        User sarah = userRepository.save(User.builder()
                .name("Sarah Smith")
                .email("sarah@example.com")
                .password("password")
                .build());

        User mike = userRepository.save(User.builder()
                .name("Mike Johnson")
                .email("mike@example.com")
                .password("password")
                .build());

        User emma = userRepository.save(User.builder()
                .name("Emma Wilson")
                .email("emma@example.com")
                .password("password")
                .build());

        // Create sample group
        ExpenseGroup roommates = ExpenseGroup.builder()
                .name("Roommates")
                .description("Shared apartment expenses")
                .createdBy(john)
                .build();
        roommates.addMember(john);
        roommates.addMember(sarah);
        roommates.addMember(mike);
        groupRepository.save(roommates);

        // Create another group
        ExpenseGroup tripGroup = ExpenseGroup.builder()
                .name("Weekend Trip")
                .description("Beach weekend getaway")
                .createdBy(sarah)
                .build();
        tripGroup.addMember(sarah);
        tripGroup.addMember(john);
        tripGroup.addMember(emma);
        tripGroup.addMember(mike);
        groupRepository.save(tripGroup);

        // Add sample expense to roommates
        Expense rent = Expense.builder()
                .group(roommates)
                .description("Monthly Rent")
                .amount(new BigDecimal("1500.00"))
                .paidBy(john)
                .splitType(SplitType.EQUAL)
                .build();

        // Equal split: $500 each
        rent.addSplit(ExpenseSplit.builder().user(john).amount(new BigDecimal("500.00")).build());
        rent.addSplit(ExpenseSplit.builder().user(sarah).amount(new BigDecimal("500.00")).build());
        rent.addSplit(ExpenseSplit.builder().user(mike).amount(new BigDecimal("500.00")).build());
        expenseRepository.save(rent);

        // Add sample expense with exact split
        Expense groceries = Expense.builder()
                .group(roommates)
                .description("Weekly Groceries")
                .amount(new BigDecimal("120.00"))
                .paidBy(sarah)
                .splitType(SplitType.EXACT)
                .build();

        groceries.addSplit(ExpenseSplit.builder().user(john).amount(new BigDecimal("50.00")).build());
        groceries.addSplit(ExpenseSplit.builder().user(sarah).amount(new BigDecimal("40.00")).build());
        groceries.addSplit(ExpenseSplit.builder().user(mike).amount(new BigDecimal("30.00")).build());
        expenseRepository.save(groceries);

        log.info("Sample data initialized successfully!");
        log.info("Sample login credentials:");
        log.info("  Email: john@example.com, Password: password");
        log.info("  Email: sarah@example.com, Password: password");
        log.info("  Email: mike@example.com, Password: password");
        log.info("  Email: emma@example.com, Password: password");
    }
}
