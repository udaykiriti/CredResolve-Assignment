package com.expenseshare.service;

import com.expenseshare.dto.BalanceDTO;
import com.expenseshare.dto.UserBalanceSummary;
import com.expenseshare.model.*;
import com.expenseshare.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    /**
     * Calculate all balances within a group.
     * Returns a list of simplified debts (who owes whom and how much).
     */
    public List<BalanceDTO> calculateGroupBalances(Long groupId) {
        // Get all expenses and settlements for the group
        List<Expense> expenses = expenseRepository.findByGroupIdWithSplits(groupId);
        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);

        // Calculate net balance for each user
        // Positive = user is owed money, Negative = user owes money
        Map<Long, BigDecimal> netBalances = new HashMap<>();
        Map<Long, User> userMap = new HashMap<>();

        // Process expenses
        for (Expense expense : expenses) {
            Long payerId = expense.getPaidBy().getId();
            userMap.put(payerId, expense.getPaidBy());

            // Payer paid the full amount, so they are owed the splits from others
            for (ExpenseSplit split : expense.getSplits()) {
                Long splitUserId = split.getUser().getId();
                userMap.put(splitUserId, split.getUser());

                if (!splitUserId.equals(payerId)) {
                    // Payer is owed this amount
                    netBalances.merge(payerId, split.getAmount(), BigDecimal::add);
                    // Split user owes this amount
                    netBalances.merge(splitUserId, split.getAmount().negate(), BigDecimal::add);
                }
            }
        }

        // Process settlements (settlements reduce debt)
        for (Settlement settlement : settlements) {
            Long payerId = settlement.getPayer().getId();
            Long payeeId = settlement.getPayee().getId();
            userMap.put(payerId, settlement.getPayer());
            userMap.put(payeeId, settlement.getPayee());

            // Payer settled some debt, so their net balance increases
            netBalances.merge(payerId, settlement.getAmount(), BigDecimal::add);
            // Payee received payment, so their net balance decreases
            netBalances.merge(payeeId, settlement.getAmount().negate(), BigDecimal::add);
        }

        // Simplify debts using greedy algorithm
        return simplifyDebts(netBalances, userMap);
    }

    /**
     * Simplify debts using a greedy algorithm.
     * Matches the maximum creditor with the maximum debtor iteratively.
     */
    private List<BalanceDTO> simplifyDebts(Map<Long, BigDecimal> netBalances, Map<Long, User> userMap) {
        List<BalanceDTO> simplifiedDebts = new ArrayList<>();

        // Create lists of creditors (positive balance) and debtors (negative balance)
        List<Map.Entry<Long, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<Long, BigDecimal>> debtors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal balance = entry.getValue();
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), balance));
            } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), balance.negate()));
            }
        }

        // Sort by amount descending
        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Match debtors with creditors
        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<Long, BigDecimal> debtor = debtors.get(i);
            Map.Entry<Long, BigDecimal> creditor = creditors.get(j);

            BigDecimal debtAmount = debtor.getValue();
            BigDecimal creditAmount = creditor.getValue();
            BigDecimal settleAmount = debtAmount.min(creditAmount);

            if (settleAmount.compareTo(new BigDecimal("0.01")) >= 0) {
                User fromUser = userMap.get(debtor.getKey());
                User toUser = userMap.get(creditor.getKey());

                simplifiedDebts.add(BalanceDTO.builder()
                        .fromUserId(debtor.getKey())
                        .fromUserName(fromUser != null ? fromUser.getName() : "Unknown")
                        .toUserId(creditor.getKey())
                        .toUserName(toUser != null ? toUser.getName() : "Unknown")
                        .amount(settleAmount)
                        .build());
            }

            // Update remaining amounts
            debtor.setValue(debtAmount.subtract(settleAmount));
            creditor.setValue(creditAmount.subtract(settleAmount));

            if (debtor.getValue().compareTo(new BigDecimal("0.01")) < 0) {
                i++;
            }
            if (creditor.getValue().compareTo(new BigDecimal("0.01")) < 0) {
                j++;
            }
        }

        return simplifiedDebts;
    }

    /**
     * Get balance summary for a specific user in a group.
     */
    public UserBalanceSummary getUserBalanceInGroup(Long userId, Long groupId) {
        List<BalanceDTO> allDebts = calculateGroupBalances(groupId);

        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : "Unknown";

        List<BalanceDTO> debts = allDebts.stream()
                .filter(b -> b.getFromUserId().equals(userId))
                .collect(Collectors.toList());

        List<BalanceDTO> credits = allDebts.stream()
                .filter(b -> b.getToUserId().equals(userId))
                .collect(Collectors.toList());

        BigDecimal totalOwed = debts.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOwing = credits.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserBalanceSummary.builder()
                .userId(userId)
                .userName(userName)
                .totalOwed(totalOwed)
                .totalOwing(totalOwing)
                .netBalance(totalOwing.subtract(totalOwed))
                .debts(debts)
                .credits(credits)
                .build();
    }

    /**
     * Get overall balance summary for a user across all groups.
     */
    public UserBalanceSummary getUserOverallBalance(Long userId) {
        List<ExpenseGroup> groups = groupRepository.findByMemberId(userId);
        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? user.getName() : "Unknown";

        List<BalanceDTO> allDebts = new ArrayList<>();
        List<BalanceDTO> allCredits = new ArrayList<>();

        for (ExpenseGroup group : groups) {
            List<BalanceDTO> groupBalances = calculateGroupBalances(group.getId());

            allDebts.addAll(groupBalances.stream()
                    .filter(b -> b.getFromUserId().equals(userId))
                    .collect(Collectors.toList()));

            allCredits.addAll(groupBalances.stream()
                    .filter(b -> b.getToUserId().equals(userId))
                    .collect(Collectors.toList()));
        }

        BigDecimal totalOwed = allDebts.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOwing = allCredits.stream()
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UserBalanceSummary.builder()
                .userId(userId)
                .userName(userName)
                .totalOwed(totalOwed)
                .totalOwing(totalOwing)
                .netBalance(totalOwing.subtract(totalOwed))
                .debts(allDebts)
                .credits(allCredits)
                .build();
    }

    /**
     * Get balance between two specific users in a group.
     */
    public BigDecimal getBalanceBetweenUsers(Long groupId, Long userId1, Long userId2) {
        List<BalanceDTO> balances = calculateGroupBalances(groupId);

        // Find debt from user1 to user2
        BigDecimal debt = balances.stream()
                .filter(b -> b.getFromUserId().equals(userId1) && b.getToUserId().equals(userId2))
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Find debt from user2 to user1
        BigDecimal credit = balances.stream()
                .filter(b -> b.getFromUserId().equals(userId2) && b.getToUserId().equals(userId1))
                .map(BalanceDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Positive = user1 owes user2, Negative = user2 owes user1
        return debt.subtract(credit);
    }
}
