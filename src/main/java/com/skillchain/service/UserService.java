package com.skillchain.service;

import com.skillchain.dao.UserDao;
import com.skillchain.dao.UserDaoImpl;
import com.skillchain.model.User;
import com.skillchain.util.DuplicateEmailException;
import com.skillchain.util.InvalidLoginException;
import com.skillchain.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * SERVICE LAYER: BUSINESS LOGIC & AUTHENTICATION
 * ============================================================================
 * Enforces business rules, string validation, password hashing, and credentials
 * verification before passing control to the DAO layer.
 */
public class UserService {

    private static final Pattern STUDENT_EMAIL_PATTERN = Pattern.compile(
            "(?i)^[a-z]+\\.[a-z]\\.\\d{4}\\.[a-z]+@rajalakshmi\\.edu\\.in$");
    private static final String EMAIL_FORMAT_HINT =
            "Use name.initial.year.department@rajalakshmi.edu.in " +
            "(for example, arjun.k.2025.cse@rajalakshmi.edu.in).";

    private final UserDao userDao;

    public UserService() {
        this(new UserDaoImpl());
    }

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Registers a new student account.
     *
     * @param name            Student full name
     * @param email           Valid email address
     * @param password        Plaintext password
     * @param confirmPassword Password confirmation
     * @return Saved User entity
     * @throws IllegalArgumentException on client validation failure
     * @throws DuplicateEmailException  if the email is already taken
     * @throws SQLException             on database failure
     */
    public User register(String name, String email, String password, String confirmPassword)
            throws DuplicateEmailException, SQLException {

        // STRING VALIDATION & DECISION MAKING:
        if (name == null || name.trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        String normalizedEmail = email == null ? "" : email.trim();
        if (!hasStudentEmailFormat(normalizedEmail)) {
            throw new IllegalArgumentException("Email format is incorrect. " + EMAIL_FORMAT_HINT);
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Check if email already exists
        if (userDao.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("An account with email '" + normalizedEmail + "' already exists.");
        }

        // A unique random salt and a deliberately slow PBKDF2 hash are stored.
        String salt = PasswordUtil.generateSalt();
        String passwordHash = PasswordUtil.hashPassword(password, salt);

        User newUser = new User();
        newUser.setName(name.trim());
        newUser.setEmail(normalizedEmail.toLowerCase());
        newUser.setPasswordHash(passwordHash);
        newUser.setSalt(salt);

        return userDao.save(newUser);
    }

    /**
     * Authenticates a student using their email and plaintext password.
     *
     * @param email    Student email
     * @param password Plaintext password
     * @return Authenticated User entity
     * @throws InvalidLoginException if credentials do not match
     * @throws SQLException          on database query error
     */
    public User login(String email, String password) throws InvalidLoginException, SQLException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new InvalidLoginException("Please provide both email and password.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!hasStudentEmailFormat(normalizedEmail)) {
            throw new InvalidLoginException("Email format is incorrect. " + EMAIL_FORMAT_HINT);
        }

        Optional<User> userOpt = userDao.findByEmail(normalizedEmail);

        // Decision making: verify user existence
        if (userOpt.isEmpty()) {
            throw new InvalidLoginException("Invalid email or password. Please try again.");
        }

        User user = userOpt.get();

        // Constant-time comparison; the verifier also supports old seeded SHA-256 records.
        boolean passwordMatches = PasswordUtil.verifyPassword(password, user.getSalt(), user.getPasswordHash());

        if (!passwordMatches) {
            throw new InvalidLoginException("Invalid email or password. Please try again.");
        }

        // Upgrade old hashes only after the correct password has been proven.
        if (PasswordUtil.needsRehash(user.getPasswordHash())) {
            String newSalt = PasswordUtil.generateSalt();
            String newHash = PasswordUtil.hashPassword(password, newSalt);
            if (!userDao.updatePasswordCredentials(user.getId(), newHash, newSalt)) {
                throw new SQLException("Password credentials could not be upgraded for this account.");
            }
            user.setPasswordHash(newHash);
            user.setSalt(newSalt);
        }

        return user;
    }

    private static boolean hasStudentEmailFormat(String email) {
        return email != null && STUDENT_EMAIL_PATTERN.matcher(email).matches();
    }
}
