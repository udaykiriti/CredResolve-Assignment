package com.expenseshare.controller;

import com.expenseshare.dto.SettlementDTO;
import com.expenseshare.model.*;
import com.expenseshare.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final GroupService groupService;
    private final BalanceService balanceService;
    private final EmailService emailService;
    private final UserService userService;

    /**
     * Record a settlement.
     */
    @PostMapping("/groups/{groupId}/settle")
    public String recordSettlement(@PathVariable Long groupId,
            @RequestParam Long payerId,
            @RequestParam Long payeeId,
            @RequestParam BigDecimal amount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            SettlementDTO dto = SettlementDTO.builder()
                    .groupId(groupId)
                    .payerId(payerId)
                    .payeeId(payeeId)
                    .amount(amount)
                    .build();

            settlementService.recordSettlement(dto);
            redirectAttributes.addFlashAttribute("success", "Settlement recorded successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + groupId;
    }

    /**
     * Delete a settlement.
     */
    @PostMapping("/settlements/{id}/delete")
    public String deleteSettlement(@PathVariable Long id,
            @RequestParam Long groupId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Settlement settlement = settlementService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Settlement not found"));

            // Only payer, payee, or group creator can delete
            ExpenseGroup group = settlement.getGroup();
            if (!settlement.getPayer().getId().equals(userId) &&
                    !settlement.getPayee().getId().equals(userId) &&
                    !group.getCreatedBy().getId().equals(userId)) {
                throw new IllegalArgumentException("You cannot delete this settlement");
            }

            settlementService.deleteSettlement(id);
            redirectAttributes.addFlashAttribute("success", "Settlement deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + groupId;
    }

    /**
     * Send a payment reminder.
     */
    @PostMapping("/groups/{groupId}/remind")
    public String sendReminder(@PathVariable Long groupId,
            @RequestParam Long toUserId,
            @RequestParam BigDecimal amount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            User fromUser = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            User toUser = userService.findById(toUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
            ExpenseGroup group = groupService.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));

            emailService.sendReminder(fromUser, toUser, amount, group);
            redirectAttributes.addFlashAttribute("success", "Reminder sent to " + toUser.getName());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + groupId;
    }
}
