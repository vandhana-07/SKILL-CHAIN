package com.skillchain.dao;

import com.skillchain.model.LearningRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * JAVA OOP CONCEPT: INTERFACE ABSTRACTION
 * ============================================================================
 * Defines data access operations for managing peer-to-peer exchange requests.
 */
public interface LearningRequestDao {

    /**
     * Persists a new learning exchange request in MySQL.
     *
     * @param request LearningRequest entity containing sender, receiver, and skills.
     * @return The persisted LearningRequest with generated primary key ID.
     * @throws SQLException if a database error occurs.
     */
    LearningRequest save(LearningRequest request) throws SQLException;

    /**
     * Retrieves all incoming exchange requests directed to this student.
     *
     * @param receiverId The student's user ID.
     * @return List of incoming LearningRequest entities with populated names.
     * @throws SQLException if a database error occurs.
     */
    List<LearningRequest> findIncomingByUserId(int receiverId) throws SQLException;

    /**
     * Retrieves all outgoing exchange requests initiated by this student.
     *
     * @param senderId The student's user ID.
     * @return List of outgoing LearningRequest entities with populated names.
     * @throws SQLException if a database error occurs.
     */
    List<LearningRequest> findOutgoingByUserId(int senderId) throws SQLException;

    /**
     * Updates a still-pending request only when the acting user is its receiver.
     *
     * @param requestId Unique request ID.
     * @param receiverId Authenticated receiver making the decision.
     * @param newStatus Target status ('ACCEPTED' or 'REJECTED').
     * @return true if updated, false if not pending, not found, or not owned by receiver.
     * @throws SQLException if a database error occurs.
     */
    boolean updateStatus(int requestId, int receiverId, String newStatus) throws SQLException;

    /**
     * Counts pending incoming requests for dashboard badge notifications.
     *
     * @param receiverId The student's user ID.
     * @return Number of PENDING requests awaiting student action.
     * @throws SQLException if a database error occurs.
     */
    int countPendingIncoming(int receiverId) throws SQLException;

    /**
     * Checks if an active request already exists between the two students for these skills.
     *
     * @param senderId         Sender ID
     * @param receiverId       Receiver ID
     * @param offeredSkillId   Skill offered
     * @param requestedSkillId Skill requested
     * @return true if an existing pending/accepted request exists
     * @throws SQLException if a database error occurs
     */
    boolean existsActiveRequest(int senderId, int receiverId, int offeredSkillId, int requestedSkillId) throws SQLException;
}
