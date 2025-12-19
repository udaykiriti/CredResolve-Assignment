package com.expenseshare.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO representing a balance between two users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceDTO {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
    private String toUserName;
    private BigDecimal amount;

    /**
     * Check if this balance represents a debt (positive amount = fromUser owes
     * toUser).
     */
    public boolean isDebt() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
