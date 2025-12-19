package com.expenseshare.controller;

import com.expenseshare.dto.UserBalanceSummary;
import com.expenseshare.model.ExpenseGroup;
import com.expenseshare.model.User;
import com.expenseshare.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final GroupService groupService;
    private final BalanceService balanceService;
    private final ExpenseService expenseService;

    /**
     * Show main dashboard.
     */
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        // Add user info to model
        AuthController.addUserToModel(model, session);

        // Get user's groups
        List<ExpenseGroup> groups = groupService.getUserGroups(userId);
        model.addAttribute("groups", groups);

        // Get overall balance summary
        UserBalanceSummary balanceSummary = balanceService.getUserOverallBalance(userId);
        model.addAttribute("balanceSummary", balanceSummary);

        // Get all users for adding to groups
        List<User> allUsers = userService.findAll();
        model.addAttribute("allUsers", allUsers);

        return "dashboard";
    }
}
