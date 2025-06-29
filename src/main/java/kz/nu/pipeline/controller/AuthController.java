package kz.nu.pipeline.controller;

import kz.nu.pipeline.annotation.AdminAccess;
import kz.nu.pipeline.model.User;
import kz.nu.pipeline.security.JwtUtil;
import kz.nu.pipeline.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates a user with email and password.
     *
     * @param username the email
     * @param password the password
     * @return a JWT token if authentication is successful
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestParam String username, @RequestParam String password) {
        Optional<User> userOptional = userService.authenticate(username, password);
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtUtil.generateToken(userOptional.get());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", username
        ));
    }

    /**
     * Logs out the current user.
     *
     * @return a success message
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logout successful");
    }

    /**
     * Registers a new user.
     *
     * @param username the email
     * @param password the password
     * @return a JWT token if registration is successful
     */
    @AdminAccess
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestParam String username,
            @RequestParam String password
    ) {
        if (userService.register(username, password)) {
            return ResponseEntity.ok(Map.of("message", "Registration successful"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
    }

    @AdminAccess
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestParam String email) {
        if (userService.deleteUser(email)) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    @GetMapping("/getMe")
    public ResponseEntity<String> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(authentication.getName());
    }
}
