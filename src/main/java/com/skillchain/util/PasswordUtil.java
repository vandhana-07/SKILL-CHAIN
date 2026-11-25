package com.skillchain.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing helpers for registration and login.
 *
 * New accounts use PBKDF2-HMAC-SHA256. The encoded hash stores its algorithm
 * and work factor so the parameters can be increased later. The verifier also
 * accepts the former salted SHA-256 format so existing accounts can log in;
 * UserService upgrades those records after a successful login.
 */
public final class PasswordUtil {

    private static final String CURRENT_ALGORITHM = "pbkdf2-sha256";
    private static final int CURRENT_ITERATIONS = 600_000;
    private static final int MIN_ACCEPTED_ITERATIONS = 10_000;
    private static final int MAX_ACCEPTED_ITERATIONS = 2_000_000;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final int DERIVED_KEY_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Returns a cryptographically random 16-byte salt as 32 lowercase hex characters. */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Derives a password hash in the format {@code pbkdf2-sha256$iterations$hexHash}.
     * The separate salt column remains a 16-byte hexadecimal value.
     */
    public static String hashPassword(String plainPassword, String saltHex) {
        if (plainPassword == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt must not be null.");
        }
        byte[] salt = parseSalt(saltHex);
        byte[] derived = derivePbkdf2(plainPassword, salt, CURRENT_ITERATIONS);
        return CURRENT_ALGORITHM + "$" + CURRENT_ITERATIONS + "$" + HexFormat.of().formatHex(derived);
    }

    /**
     * Verifies both current PBKDF2 hashes and legacy salted SHA-256 hashes.
     * Malformed stored values fail closed and return {@code false}.
     */
    public static boolean verifyPassword(String rawPassword, String saltHex, String storedHash) {
        if (rawPassword == null || saltHex == null || storedHash == null) {
            return false;
        }

        final byte[] salt;
        try {
            salt = parseSalt(saltHex);
        } catch (IllegalArgumentException malformedSalt) {
            return false;
        }

        if (storedHash.startsWith(CURRENT_ALGORITHM + "$")) {
            String[] parts = storedHash.split("\\$", -1);
            if (parts.length != 3) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                if (iterations < MIN_ACCEPTED_ITERATIONS || iterations > MAX_ACCEPTED_ITERATIONS) {
                    return false;
                }
                byte[] expected = HexFormat.of().parseHex(parts[2]);
                if (expected.length != DERIVED_KEY_BITS / 8) {
                    return false;
                }
                byte[] actual = derivePbkdf2(rawPassword, salt, iterations);
                return MessageDigest.isEqual(actual, expected);
            } catch (IllegalArgumentException malformedHash) {
                return false;
            }
        }

        // Backward compatibility for the previous SHA-256(salt || password) format.
        try {
            byte[] expected = HexFormat.of().parseHex(storedHash);
            if (expected.length != 32) {
                return false;
            }
            byte[] actual = legacySha256(rawPassword, salt);
            return MessageDigest.isEqual(actual, expected);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }

    /** True when a valid login should replace this hash with the current format/work factor. */
    public static boolean needsRehash(String storedHash) {
        if (storedHash == null) {
            return true;
        }
        String[] parts = storedHash.split("\\$", -1);
        if (parts.length != 3 || !CURRENT_ALGORITHM.equals(parts[0])) {
            return true;
        }
        try {
            return Integer.parseInt(parts[1]) != CURRENT_ITERATIONS
                    || HexFormat.of().parseHex(parts[2]).length != DERIVED_KEY_BITS / 8;
        } catch (IllegalArgumentException malformedHash) {
            return true;
        }
    }

    private static byte[] parseSalt(String saltHex) {
        final byte[] salt;
        try {
            salt = HexFormat.of().parseHex(saltHex);
        } catch (IllegalArgumentException malformedSalt) {
            throw new IllegalArgumentException("Salt must be hexadecimal.", malformedSalt);
        }
        if (salt.length != SALT_BYTE_LENGTH) {
            throw new IllegalArgumentException("Salt must contain exactly 16 bytes.");
        }
        return salt;
    }

    private static byte[] derivePbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, DERIVED_KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable on this JVM.", e);
        } finally {
            keySpec.clearPassword();
        }
    }

    private static byte[] legacySha256(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable on this JVM.", e);
        }
    }
}
