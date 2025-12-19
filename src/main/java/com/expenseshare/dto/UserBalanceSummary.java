package com.expenseshare.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for user balance summary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBalanceSummary {
    private Long userId;
    private String userName;
    private BigDecimal totalOwed; // Total amount user owes to others
    private BigDecimal totalOwing; // Total amount others owe to user
    private BigDecimal netBalance; // Net balance (positive = others owe you)
    private List<BalanceDTO> debts; // Detailed debts
    private List<BalanceDTO> credits; // Detailed credits

    /**
     * Check if user is all settled (net balance is zero).
     */
    public boolean isSettled() {
        return netBalance != null && netBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}
