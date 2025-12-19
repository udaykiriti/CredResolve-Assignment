package com.expenseshare.controller;

import com.expenseshare.dto.BalanceDTO;
import com.expenseshare.dto.UserBalanceSummary;
import com.expenseshare.model.User;
import com.expenseshare.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for AJAX calls.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;
    private final GroupService groupService;
    private final BalanceService balanceService;

    /**
     * Search users by email or name.
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<User> users = userService.searchUsers(query);
        return ResponseEntity.ok(users);
    }

    /**
     * Get group balances.
     */
    @GetMapping("/groups/{groupId}/balances")
    public ResponseEntity<List<BalanceDTO>> getGroupBalances(@PathVariable Long groupId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        if (!groupService.isMember(groupId, userId)) {
            return ResponseEntity.status(403).build();
        }

        List<BalanceDTO> balances = balanceService.calculateGroupBalances(groupId);
        return ResponseEntity.ok(balances);
    }

    /**
     * Get user balance summary for a group.
     */
    @GetMapping("/groups/{groupId}/balances/user")
    public ResponseEntity<UserBalanceSummary> getUserBalance(@PathVariable Long groupId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        if (!groupService.isMember(groupId, userId)) {
            return ResponseEntity.status(403).build();
        }

        UserBalanceSummary summary = balanceService.getUserBalanceInGroup(userId, groupId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get overall user balance.
     */
    @GetMapping("/users/balance")
    public ResponseEntity<UserBalanceSummary> getOverallBalance(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        UserBalanceSummary summary = balanceService.getUserOverallBalance(userId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Check if user exists by email.
     */
    @GetMapping("/users/check")
    public ResponseEntity<Map<String, Object>> checkUserExists(@RequestParam String email,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> response = new HashMap<>();
        userService.findByEmail(email).ifPresentOrElse(
                user -> {
                    response.put("exists", true);
                    response.put("name", user.getName());
                    response.put("id", user.getId());
                },
                () -> response.put("exists", false));

        return ResponseEntity.ok(response);
    }
}
