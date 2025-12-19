package com.expenseshare.controller;

import com.expenseshare.dto.BalanceDTO;
import com.expenseshare.dto.GroupDTO;
import com.expenseshare.dto.UserBalanceSummary;
import com.expenseshare.model.*;
import com.expenseshare.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final UserService userService;
    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final BalanceService balanceService;
    private final SettlementService settlementService;

    /**
     * List all groups for current user.
     */
    @GetMapping
    public String listGroups(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        AuthController.addUserToModel(model, session);

        List<ExpenseGroup> groups = groupService.getUserGroups(userId);
        model.addAttribute("groups", groups);

        return "groups/list";
    }

    /**
     * Show create group form.
     */
    @GetMapping("/new")
    public String createGroupForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        AuthController.addUserToModel(model, session);
        model.addAttribute("groupDTO", new GroupDTO());

        // Get all users for member selection
        List<User> allUsers = userService.findAll();
        model.addAttribute("allUsers", allUsers);

        return "groups/create";
    }

    /**
     * Create a new group.
     */
    @PostMapping
    public String createGroup(@ModelAttribute GroupDTO groupDTO,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            ExpenseGroup group = groupService.createGroup(groupDTO, currentUser);
            redirectAttributes.addFlashAttribute("success", "Group created successfully!");
            return "redirect:/groups/" + group.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/groups/new";
        }
    }

    /**
     * Show group detail page.
     */
    @GetMapping("/{id}")
    public String groupDetail(@PathVariable Long id,
            HttpSession session,
            Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        ExpenseGroup group = groupService.findByIdWithMembers(id);
        if (group == null) {
            return "redirect:/groups";
        }

        // Check if user is a member
        if (!groupService.isMember(id, userId)) {
            return "redirect:/groups";
        }

        AuthController.addUserToModel(model, session);

        // Get group data
        model.addAttribute("group", group);

        // Get expenses
        List<Expense> expenses = expenseService.getGroupExpenses(id);
        model.addAttribute("expenses", expenses);

        // Get settlements
        List<Settlement> settlements = settlementService.getGroupSettlements(id);
        model.addAttribute("settlements", settlements);

        // Get balances
        List<BalanceDTO> balances = balanceService.calculateGroupBalances(id);
        model.addAttribute("balances", balances);

        // Get user's balance in this group
        UserBalanceSummary userBalance = balanceService.getUserBalanceInGroup(userId, id);
        model.addAttribute("userBalance", userBalance);

        // Get all users for adding members
        List<User> allUsers = userService.findAll();
        model.addAttribute("allUsers", allUsers);

        return "groups/detail";
    }

    /**
     * Add a member to the group.
     */
    @PostMapping("/{id}/members")
    public String addMember(@PathVariable Long id,
            @RequestParam String email,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            groupService.addMemberByEmail(id, email);
            redirectAttributes.addFlashAttribute("success", "Member added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + id;
    }

    /**
     * Remove a member from the group.
     */
    @PostMapping("/{id}/members/{memberId}/remove")
    public String removeMember(@PathVariable Long id,
            @PathVariable Long memberId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            groupService.removeMember(id, memberId);
            redirectAttributes.addFlashAttribute("success", "Member removed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups/" + id;
    }

    /**
     * Delete a group.
     */
    @PostMapping("/{id}/delete")
    public String deleteGroup(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            ExpenseGroup group = groupService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));

            // Only creator can delete
            if (!group.getCreatedBy().getId().equals(userId)) {
                throw new IllegalArgumentException("Only the group creator can delete the group");
            }

            groupService.deleteGroup(id);
            redirectAttributes.addFlashAttribute("success", "Group deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/groups";
    }
}
