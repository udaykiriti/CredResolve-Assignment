package com.expenseshare.service;

import com.expenseshare.model.User;
import com.expenseshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * Register a new user.
     */
    public User register(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .name(name)
                .email(email.toLowerCase().trim())
                .password(password) // In production, hash this!
                .build();

        return userRepository.save(user);
    }

    /**
     * Authenticate a user.
     */
    public Optional<User> authenticate(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email.toLowerCase().trim());
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }

    /**
     * Find user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by email.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Get all users.
     */
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Search users by email or name.
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        return userRepository.searchByEmailOrName(query);
    }

    /**
     * Find users by IDs.
     */
    @Transactional(readOnly = true)
    public List<User> findByIds(List<Long> ids) {
        return userRepository.findByIdIn(ids);
    }

    /**
     * Update user profile.
     */
    public User updateProfile(Long userId, String name, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(name);
        if (!user.getEmail().equals(email.toLowerCase().trim())) {
            if (userRepository.existsByEmail(email.toLowerCase().trim())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(email.toLowerCase().trim());
        }

        return userRepository.save(user);
    }
}
