package com.expenseshare.dto;

import com.expenseshare.model.SplitType;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for creating a new expense.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDTO {
    private Long groupId;
    private String description;
    private BigDecimal amount;
    private Long paidById;
    private SplitType splitType;

    /**
     * For EQUAL split: list of user IDs to split among.
     * For EXACT split: map of userId -> exact amount.
     * For PERCENTAGE split: map of userId -> percentage.
     */
    private List<Long> splitAmongUserIds;
    private Map<Long, BigDecimal> exactAmounts;
    private Map<Long, BigDecimal> percentages;
}
