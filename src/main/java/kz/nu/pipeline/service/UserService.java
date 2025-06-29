package kz.nu.pipeline.service;

import kz.nu.pipeline.model.User;
import kz.nu.pipeline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get the current user from the security context.
     *
     * @return an Optional containing the current user if authenticated, or empty if not authenticated
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        String username = authentication.getName();
        return this.getUserByUsername(username);
    }

    /**
     * Authenticate a user with email and password.
     *
     * @param username the email
     * @param password the password
     */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOptional = this.getUserByUsername(username);
        return userOptional
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }

    /**
     * Register a new user.
     *
     * @param username the username
     * @param password the password
     * @return true if registration is successful, false otherwise
     */
    public boolean register(String username, String password) {
        if (this.getUserByUsername(username).isPresent()) {
            return false;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .isAdmin(false)
                .build());
        return true;
    }

    public boolean deleteUser(String username) {
        Optional<User> user = this.getUserByUsername(username);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return true;
        }
        return false;
    }
}
