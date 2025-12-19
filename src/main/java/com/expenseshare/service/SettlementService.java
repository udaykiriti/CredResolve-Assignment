package com.expenseshare.service;

import com.expenseshare.dto.SettlementDTO;
import com.expenseshare.model.*;
import com.expenseshare.repository.GroupRepository;
import com.expenseshare.repository.SettlementRepository;
import com.expenseshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Record a settlement payment.
     */
    public Settlement recordSettlement(SettlementDTO dto) {
        ExpenseGroup group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User payer = userRepository.findById(dto.getPayerId())
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));

        User payee = userRepository.findById(dto.getPayeeId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));

        if (dto.getPayerId().equals(dto.getPayeeId())) {
            throw new IllegalArgumentException("Payer and payee cannot be the same");
        }

        Settlement settlement = Settlement.builder()
                .group(group)
                .payer(payer)
                .payee(payee)
                .amount(dto.getAmount())
                .build();

        Settlement saved = settlementRepository.save(settlement);

        // Send email notifications
        emailService.sendSettlementNotification(saved);

        return saved;
    }

    /**
     * Get settlement by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Settlement> findById(Long id) {
        return settlementRepository.findById(id);
    }

    /**
     * Get all settlements for a group.
     */
    @Transactional(readOnly = true)
    public List<Settlement> getGroupSettlements(Long groupId) {
        return settlementRepository.findByGroupId(groupId);
    }

    /**
     * Get all settlements involving a user.
     */
    @Transactional(readOnly = true)
    public List<Settlement> getUserSettlements(Long userId) {
        return settlementRepository.findByUserId(userId);
    }

    /**
     * Get settlements for a user in a specific group.
     */
    @Transactional(readOnly = true)
    public List<Settlement> getGroupSettlementsForUser(Long groupId, Long userId) {
        return settlementRepository.findByGroupIdAndUserId(groupId, userId);
    }

    /**
     * Delete a settlement.
     */
    public void deleteSettlement(Long settlementId) {
        settlementRepository.deleteById(settlementId);
    }
}
