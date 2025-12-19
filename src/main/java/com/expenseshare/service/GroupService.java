package com.expenseshare.service;

import com.expenseshare.dto.GroupDTO;
import com.expenseshare.model.ExpenseGroup;
import com.expenseshare.model.User;
import com.expenseshare.repository.GroupRepository;
import com.expenseshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    /**
     * Create a new expense group.
     */
    public ExpenseGroup createGroup(String name, String description, User createdBy) {
        ExpenseGroup group = ExpenseGroup.builder()
                .name(name)
                .description(description)
                .createdBy(createdBy)
                .build();

        // Add creator as a member
        group.addMember(createdBy);

        return groupRepository.save(group);
    }

    /**
     * Create group from DTO.
     */
    public ExpenseGroup createGroup(GroupDTO dto, User createdBy) {
        ExpenseGroup group = createGroup(dto.getName(), dto.getDescription(), createdBy);

        // Add members by email
        if (dto.getMemberEmails() != null && !dto.getMemberEmails().isBlank()) {
            String[] emails = dto.getMemberEmails().split(",");
            Arrays.stream(emails)
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .forEach(email -> {
                        userRepository.findByEmail(email.toLowerCase())
                                .ifPresent(user -> addMember(group.getId(), user));
                    });
        }

        return group;
    }

    /**
     * Get group by ID.
     */
    @Transactional(readOnly = true)
    public Optional<ExpenseGroup> findById(Long id) {
        return groupRepository.findById(id);
    }

    /**
     * Get group by ID with members loaded.
     */
    @Transactional(readOnly = true)
    public ExpenseGroup findByIdWithMembers(Long id) {
        return groupRepository.findByIdWithMembers(id);
    }

    /**
     * Get all groups for a user.
     */
    @Transactional(readOnly = true)
    public List<ExpenseGroup> getUserGroups(Long userId) {
        return groupRepository.findByMemberId(userId);
    }

    /**
     * Add a member to a group.
     */
    public ExpenseGroup addMember(Long groupId, User user) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        group.addMember(user);
        return groupRepository.save(group);
    }

    /**
     * Add member by email.
     */
    public ExpenseGroup addMemberByEmail(Long groupId, String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        return addMember(groupId, user);
    }

    /**
     * Remove a member from a group.
     */
    public ExpenseGroup removeMember(Long groupId, Long userId) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Don't allow removing the creator
        if (group.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot remove the group creator");
        }

        group.removeMember(user);
        return groupRepository.save(group);
    }

    /**
     * Update group details.
     */
    public ExpenseGroup updateGroup(Long groupId, String name, String description) {
        ExpenseGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        group.setName(name);
        group.setDescription(description);

        return groupRepository.save(group);
    }

    /**
     * Delete a group.
     */
    public void deleteGroup(Long groupId) {
        groupRepository.deleteById(groupId);
    }

    /**
     * Check if user is a member of the group.
     */
    @Transactional(readOnly = true)
    public boolean isMember(Long groupId, Long userId) {
        ExpenseGroup group = groupRepository.findByIdWithMembers(groupId);
        if (group == null)
            return false;
        return group.getMembers().stream()
                .anyMatch(m -> m.getId().equals(userId));
    }
}
