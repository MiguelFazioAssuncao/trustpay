package com.trustpay.backend.services;

/**
 * Manages user authentication and registration.
 */
public interface AuthService {

    /**
     * Authenticates a user with an email and password.
     *
     * @param email the user's email
     * @param password the user's password
     * @return JWT authentication token
     * @throws org.springframework.security.authentication.BadCredentialsException if the credentials are invalid
     */
    String login(String email, String password);

    /**
     * Registers a new user in the system.
     *
     * @param email the new user's email (must be unique)
     * @param password the new user's password
     * @return JWT token for the newly authenticated user
     * @throws org.springframework.dao.DataIntegrityViolationException if the email is already in use
     */
    String register(String email, String password);
}
