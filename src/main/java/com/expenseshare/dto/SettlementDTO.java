package com.expenseshare.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for creating a settlement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDTO {
    private Long groupId;
    private Long payerId;
    private Long payeeId;
    private BigDecimal amount;
}
