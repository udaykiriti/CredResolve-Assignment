package com.expenseshare.service;

import com.expenseshare.dto.ExpenseDTO;
import com.expenseshare.model.*;
import com.expenseshare.repository.ExpenseRepository;
import com.expenseshare.repository.GroupRepository;
import com.expenseshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Add a new expense with splits calculated based on split type.
     */
    public Expense addExpense(ExpenseDTO dto) {
        ExpenseGroup group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User paidBy = userRepository.findById(dto.getPaidById())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));

        Expense expense = Expense.builder()
                .group(group)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .paidBy(paidBy)
                .splitType(dto.getSplitType())
                .build();

        // Calculate and add splits based on split type
        switch (dto.getSplitType()) {
            case EQUAL:
                addEqualSplits(expense, dto.getSplitAmongUserIds());
                break;
            case EXACT:
                addExactSplits(expense, dto.getExactAmounts());
                break;
            case PERCENTAGE:
                addPercentageSplits(expense, dto.getPercentages());
                break;
        }

        Expense saved = expenseRepository.save(expense);

        // Send email notifications
        emailService.sendExpenseNotification(saved);

        return saved;
    }

    /**
     * Calculate and add equal splits among users.
     */
    private void addEqualSplits(Expense expense, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new IllegalArgumentException("At least one user required for split");
        }

        List<User> users = userRepository.findByIdIn(userIds);
        BigDecimal splitAmount = expense.getAmount()
                .divide(BigDecimal.valueOf(users.size()), 2, RoundingMode.HALF_UP);

        // Adjust for rounding - last person gets the remainder
        BigDecimal total = splitAmount.multiply(BigDecimal.valueOf(users.size()));
        BigDecimal remainder = expense.getAmount().subtract(total);

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            BigDecimal amount = (i == users.size() - 1)
                    ? splitAmount.add(remainder)
                    : splitAmount;

            ExpenseSplit split = ExpenseSplit.builder()
                    .user(user)
                    .amount(amount)
                    .build();
            expense.addSplit(split);
        }
    }

    /**
     * Add exact amount splits.
     */
    private void addExactSplits(Expense expense, Map<Long, BigDecimal> exactAmounts) {
        if (exactAmounts == null || exactAmounts.isEmpty()) {
            throw new IllegalArgumentException("Exact amounts required for EXACT split");
        }

        // Validate total matches expense amount
        BigDecimal total = exactAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(expense.getAmount()) != 0) {
            throw new IllegalArgumentException(
                    "Exact amounts total (" + total + ") must equal expense amount (" + expense.getAmount() + ")");
        }

        for (Map.Entry<Long, BigDecimal> entry : exactAmounts.entrySet()) {
            User user = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + entry.getKey()));

            ExpenseSplit split = ExpenseSplit.builder()
                    .user(user)
                    .amount(entry.getValue())
                    .build();
            expense.addSplit(split);
        }
    }

    /**
     * Add percentage-based splits.
     */
    private void addPercentageSplits(Expense expense, Map<Long, BigDecimal> percentages) {
        if (percentages == null || percentages.isEmpty()) {
            throw new IllegalArgumentException("Percentages required for PERCENTAGE split");
        }

        // Validate percentages sum to 100
        BigDecimal totalPercentage = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException(
                    "Percentages must sum to 100, got: " + totalPercentage);
        }

        BigDecimal runningTotal = BigDecimal.ZERO;
        List<Long> userIds = List.copyOf(percentages.keySet());

        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            BigDecimal percentage = percentages.get(userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            BigDecimal amount;
            if (i == userIds.size() - 1) {
                // Last user gets remainder to avoid rounding issues
                amount = expense.getAmount().subtract(runningTotal);
            } else {
                amount = expense.getAmount()
                        .multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                runningTotal = runningTotal.add(amount);
            }

            ExpenseSplit split = ExpenseSplit.builder()
                    .user(user)
                    .amount(amount)
                    .percentage(percentage)
                    .build();
            expense.addSplit(split);
        }
    }

    /**
     * Get expense by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    /**
     * Get expense by ID with splits loaded.
     */
    @Transactional(readOnly = true)
    public Expense findByIdWithSplits(Long id) {
        return expenseRepository.findByIdWithSplits(id);
    }

    /**
     * Get all expenses for a group.
     */
    @Transactional(readOnly = true)
    public List<Expense> getGroupExpenses(Long groupId) {
        return expenseRepository.findByGroupIdWithSplits(groupId);
    }

    /**
     * Delete an expense.
     */
    public void deleteExpense(Long expenseId) {
        expenseRepository.deleteById(expenseId);
    }

    /**
     * Get expenses paid by a user.
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesPaidByUser(Long userId) {
        return expenseRepository.findByPaidById(userId);
    }
}
