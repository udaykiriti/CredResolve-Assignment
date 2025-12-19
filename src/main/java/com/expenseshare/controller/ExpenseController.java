package com.expenseshare.controller;

import com.expenseshare.dto.ExpenseDTO;
import com.expenseshare.model.*;
import com.expenseshare.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ExpenseController {

    private final UserService userService;
    private final GroupService groupService;
    private final ExpenseService expenseService;

    /**
     * Show add expense form.
     */
    @GetMapping("/groups/{groupId}/expenses/new")
    public String addExpenseForm(@PathVariable Long groupId,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        ExpenseGroup group = groupService.findByIdWithMembers(groupId);
        if (group == null || !groupService.isMember(groupId, userId)) {
            return "redirect:/groups";
        }

        AuthController.addUserToModel(model, session);
        model.addAttribute("group", group);
        model.addAttribute("splitTypes", SplitType.values());

        return "expenses/form";
    }

    /**
     * Add a new expense.
     */
    @PostMapping("/groups/{groupId}/expenses")
    public String addExpense(@PathVariable Long groupId,
            @RequestParam String description,
            @RequestParam BigDecimal amount,
            @RequestParam Long paidById,
            @RequestParam SplitType splitType,
            @RequestParam(required = false) List<Long> splitAmongUserIds,
            @RequestParam Map<String, String> allParams,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            ExpenseDTO dto = ExpenseDTO.builder()
                    .groupId(groupId)
                    .description(description)
                    .amount(amount)
                    .paidById(paidById)
                    .splitType(splitType)
                    .build();

            switch (splitType) {
                case EQUAL:
                    if (splitAmongUserIds == null || splitAmongUserIds.isEmpty()) {
                        throw new IllegalArgumentException("Select at least one member for equal split");
                    }
                    dto.setSplitAmongUserIds(splitAmongUserIds);
                    break;

                case EXACT:
                    Map<Long, BigDecimal> exactAmounts = new HashMap<>();
                    for (Map.Entry<String, String> entry : allParams.entrySet()) {
                        if (entry.getKey().startsWith("exactAmount_")) {
                            String userIdStr = entry.getKey().replace("exactAmount_", "");
                            BigDecimal exactAmount = new BigDecimal(entry.getValue());
                            if (exactAmount.compareTo(BigDecimal.ZERO) > 0) {
                                exactAmounts.put(Long.parseLong(userIdStr), exactAmount);
                            }
                        }
                    }
                    if (exactAmounts.isEmpty()) {
                        throw new IllegalArgumentException("Enter at least one amount for exact split");
                    }
                    dto.setExactAmounts(exactAmounts);
                    break;

                case PERCENTAGE:
                    Map<Long, BigDecimal> percentages = new HashMap<>();
                    for (Map.Entry<String, String> entry : allParams.entrySet()) {
                        if (entry.getKey().startsWith("percentage_")) {
                            String userIdStr = entry.getKey().replace("percentage_", "");
                            BigDecimal percentage = new BigDecimal(entry.getValue());
                            if (percentage.compareTo(BigDecimal.ZERO) > 0) {
                                percentages.put(Long.parseLong(userIdStr), percentage);
                            }
                        }
                    }
                    if (percentages.isEmpty()) {
                        throw new IllegalArgumentException("Enter at least one percentage");
                    }
                    dto.setPercentages(percentages);
                    break;
            }

            expenseService.addExpense(dto);
            redirectAttributes.addFlashAttribute("success", "Expense added successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + groupId;
    }

    /**
     * Delete an expense.
     */
    @PostMapping("/expenses/{id}/delete")
    public String deleteExpense(@PathVariable Long id,
            @RequestParam Long groupId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Expense expense = expenseService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

            // Only payer or group creator can delete
            ExpenseGroup group = expense.getGroup();
            if (!expense.getPaidBy().getId().equals(userId) &&
                    !group.getCreatedBy().getId().equals(userId)) {
                throw new IllegalArgumentException("You cannot delete this expense");
            }

            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute("success", "Expense deleted successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + groupId;
    }
}
