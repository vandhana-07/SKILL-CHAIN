-- ============================================================================
-- SKILL CHAIN: A Skill Exchange Platform For Students
-- Database Schema Script: schema.sql
-- Database Management System: MySQL 8.0+
-- ============================================================================

-- Create database if it doesn't already exist
CREATE DATABASE IF NOT EXISTS skill_chain_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE skill_chain_db;

-- ----------------------------------------------------------------------------
-- Table 1: users
-- Stores student account profile details, salted password hashes, and timestamps.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_email_format CHECK (email LIKE '%@%._%')
) ENGINE=InnoDB;

-- Idempotent compatibility migration for databases created by earlier versions.
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(120) NOT NULL;

-- Keep the bundled demo accounts usable after strict college-email validation.
UPDATE users SET email = CASE email
    WHEN 'alice@university.edu' THEN 'alice.j.2025.cse@rajalakshmi.edu.in'
    WHEN 'bob@university.edu' THEN 'bob.s.2025.ece@rajalakshmi.edu.in'
    WHEN 'charlie@university.edu' THEN 'charlie.d.2024.bt@rajalakshmi.edu.in'
    WHEN 'diana@university.edu' THEN 'diana.p.2023.mec@rajalakshmi.edu.in'
    ELSE email
END
WHERE email IN (
    'alice@university.edu', 'bob@university.edu',
    'charlie@university.edu', 'diana@university.edu'
);

-- ----------------------------------------------------------------------------
-- Table 2: skills
-- Global catalog of skills organized by academic & technical category.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skills (
    skill_id INT AUTO_INCREMENT PRIMARY KEY,
    skill_name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 3: user_skills
-- Junction table connecting users to skills they can TEACH or want to LEARN.
-- Unique constraint ensures no duplicate TEACH or LEARN entry per user & skill.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_skills (
    user_skill_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    skill_id INT NOT NULL,
    skill_type ENUM('TEACH', 'LEARN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) 
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) 
        REFERENCES skills(skill_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_skill_type UNIQUE (user_id, skill_id, skill_type),
    KEY idx_user_skills_user_type (user_id, skill_type, skill_id),
    KEY idx_user_skills_skill_type (skill_id, skill_type, user_id)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 4: learning_requests
-- Records swap invitations between students for reciprocal skill learning.
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS learning_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    offered_skill_id INT NOT NULL,
    requested_skill_id INT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_request_sender FOREIGN KEY (sender_id) 
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_request_receiver FOREIGN KEY (receiver_id) 
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_request_offered_skill FOREIGN KEY (offered_skill_id) 
        REFERENCES skills(skill_id) ON DELETE CASCADE,
    CONSTRAINT fk_request_requested_skill FOREIGN KEY (requested_skill_id) 
        REFERENCES skills(skill_id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_request CHECK (sender_id <> receiver_id),
    KEY idx_requests_receiver_status_time (receiver_id, status, created_at),
    KEY idx_requests_sender_status_time (sender_id, status, created_at)
) ENGINE=InnoDB;

-- Add the approval timestamp to existing databases without dropping records.
SET @approved_at_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'learning_requests'
      AND COLUMN_NAME = 'approved_at'
);
SET @approved_at_ddl = IF(
    @approved_at_exists = 0,
    'ALTER TABLE learning_requests ADD COLUMN approved_at TIMESTAMP NULL DEFAULT NULL AFTER created_at',
    'SELECT 1'
);
PREPARE approved_at_stmt FROM @approved_at_ddl;
EXECUTE approved_at_stmt;
DEALLOCATE PREPARE approved_at_stmt;

-- ============================================================================
-- SEED DATA (Pre-populated sample data for testing and demonstration)
-- All sample users share the login password: Password123!
-- ============================================================================

-- 10 Standard Skills Across Categories
INSERT IGNORE INTO skills (skill_id, skill_name, category) VALUES
(1, 'Java', 'Programming'),
(2, 'Python', 'Programming'),
(3, 'C++', 'Programming'),
(4, 'JavaScript', 'Web Development'),
(5, 'React', 'Web Development'),
(6, 'Node.js', 'Web Development'),
(7, 'SQL', 'Database & Data'),
(8, 'Machine Learning', 'Database & Data'),
(9, 'UI/UX Design', 'Design & Creative'),
(10, 'Graphic Design', 'Design & Creative');

-- 4 Sample Users (Password for all 4 is: Password123!)
INSERT IGNORE INTO users (user_id, name, email, password_hash, salt) VALUES
(1, 'Alice Johnson', 'alice.j.2025.cse@rajalakshmi.edu.in', '387e62dc1e72c9e88611fe78a51a12297cfaa858e3ac9db8ca15d110e4453826', 'ef8c7f2555b56cced090ce0b04fd574e'),
(2, 'Bob Smith', 'bob.s.2025.ece@rajalakshmi.edu.in', '286c31d089fe148b8eb04191ce8c1b25f382129cacc7c641b55ed3679fd1a9f7', 'ae778d68f8948c531880886ad7837721'),
(3, 'Charlie Davis', 'charlie.d.2024.bt@rajalakshmi.edu.in', '80372e49e43420718b1130b66d2786605029f4435ac131e4c443e4e9194f8090', '17fc8b95ec6f0a3f991215ec87b4a24e'),
(4, 'Diana Prince', 'diana.p.2023.mec@rajalakshmi.edu.in', '0da212a69ba70527f48d67a607732d17dc6e17db8aa77b25ba95e2309a1e8894', '00287b6de26009d160fb6837586cc4b4');

-- Sample User Skills to establish immediate Reciprocal Matches:
-- Match Pair 1: Alice & Bob
-- Alice teaches Java (1), wants Python (2)
-- Bob teaches Python (2), wants Java (1)
-- Match Pair 2: Charlie & Diana
-- Charlie teaches React (5), wants SQL (7)
-- Diana teaches SQL (7), wants React (5)
INSERT IGNORE INTO user_skills (user_id, skill_id, skill_type) VALUES
-- Alice
(1, 1, 'TEACH'),
(1, 2, 'LEARN'),
(1, 3, 'TEACH'),
(1, 9, 'LEARN'),
-- Bob
(2, 2, 'TEACH'),
(2, 1, 'LEARN'),
(2, 4, 'TEACH'),
(2, 10, 'LEARN'),
-- Charlie
(3, 5, 'TEACH'),
(3, 7, 'LEARN'),
(3, 6, 'TEACH'),
-- Diana
(4, 7, 'TEACH'),
(4, 5, 'LEARN'),
(4, 8, 'TEACH');

-- Sample Learning Requests for immediate demonstration:
-- 1. Alice proposed swap with Bob (Java for Python) -> ACCEPTED
-- 2. Charlie proposed swap with Alice (React for Java) -> PENDING
INSERT IGNORE INTO learning_requests (request_id, sender_id, receiver_id, offered_skill_id, requested_skill_id, status, approved_at) VALUES
(1, 1, 2, 1, 2, 'ACCEPTED', CURRENT_TIMESTAMP),
(2, 3, 1, 5, 1, 'PENDING', NULL);

