package com.innowise.userservice.service;

import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.UserDeactivationNotAllowedException;
import com.innowise.userservice.exception.UserDeletionNotAllowedException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User business logic: CRUD, soft deletion, activation.
 *
 * <p>All methods operate on {@link UserResponseDTO} — JPA entities never leak
 * outside the service layer.
 */
public interface UserService {

    /**
     * Retrieves an active user by ID.
     *
     * @param id User ID
     * @return user response DTO
     * @throws UserNotFoundException if user not found or inactive
     */
    UserResponseDTO getUserById(Long id);

    /**
     * Creates a new user.
     *
     * @param dto user request data with validation constraints
     * @return created user response DTO
     * @throws DuplicateEmailException if email already exists
     */
    UserResponseDTO createUser(UserRequestDTO dto);

    /**
     * Updates an active user.
     *
     * @param id  User ID
     * @param dto user request data
     * @return updated user response DTO
     * @throws UserNotFoundException   if user not found or inactive
     * @throws DuplicateEmailException if email already exists
     */
    UserResponseDTO updateUser(Long id, UserRequestDTO dto);

    /**
     * Hard deletes a user.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Only inactive users (active=false) can be deleted</li>
     *   <li>Attempting to delete an active user throws UserDeletionNotAllowedException</li>
     * </ul>
     *
     * @param id User ID
     * @throws UserNotFoundException          if user not found
     * @throws UserDeletionNotAllowedException if user is still active
     */
    void deleteUser(Long id);

    /**
     * Filters users by criteria with pagination.
     *
     * @param filter   filter criteria (name, surname, active)
     * @param pageable pagination parameters
     * @return paginated user response DTOs
     */
    Page<UserResponseDTO> filterUsers(UserFilterDTO filter, Pageable pageable);

    /**
     * Activates a user.
     *
     * @param id User ID
     * @return activated user response DTO
     * @throws UserNotFoundException if user not found
     */
    UserResponseDTO activateUser(Long id);

    /**
     * Deactivates a user (soft delete).
     *
     * <p>Business rules:
     * <ul>
     *   <li>User can only be deactivated if they have no active payment cards</li>
     *   <li>Deactivate all cards first, then deactivate the user</li>
     * </ul>
     *
     * @param id User ID
     * @throws UserNotFoundException                if user not found
     * @throws UserDeactivationNotAllowedException  if user has active payment cards
     */
    UserResponseDTO deactivateUser(Long id);
}
