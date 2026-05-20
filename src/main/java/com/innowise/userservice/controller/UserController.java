package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user operations.
 *
 * <p>Provides CRUD operations for users plus activate/deactivate functionality.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    /**
     * Creates a new user.
     *
     * @param dto the user request data
     * @return the created user with 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a user and returns the created user data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponseDTO> createUser(
        @Parameter(description = "User data to create") @Valid @RequestBody UserRequestDTO dto
    ) {
        UserResponseDTO created = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a user by ID.
     *
     * @param id the user ID
     * @return the user data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> getUserById(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Gets all users with optional filtering.
     *
     * @param filter the filter criteria
     * @param pageable pagination information
     * @return page of users
     */
    @GetMapping
    @Operation(summary = "Get users with filter", description = "Retrieves users with optional filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    })
    public ResponseEntity<Page<UserResponseDTO>> filterUsers(
        @Parameter(description = "Filter criteria") UserFilterDTO filter,
        Pageable pageable
    ) {
        Page<UserResponseDTO> users = userService.filterUsers(filter, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Updates an existing user.
     *
     * @param id the user ID
     * @param dto the updated user data
     * @return the updated user
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user's data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserResponseDTO> updateUser(
        @Parameter(description = "User ID") @PathVariable Long id,
        @Parameter(description = "Updated user data") @Valid @RequestBody UserRequestDTO dto
    ) {
        UserResponseDTO updated = userService.updateUser(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates a user (soft delete).
     *
     * @param id the user ID
     * @return 204 status
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft deletes a user by setting active=false")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a user.
     *
     * @param id the user ID
     * @return the activated user
     */
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate user", description = "Activates a deactivated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> activateUser(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        UserResponseDTO activated = userService.activateUser(id);
        return ResponseEntity.ok(activated);
    }

    /**
     * Deactivates a user.
     *
     * @param id the user ID
     * @return the deactivated user
     */
    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivates an active user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> deactivateUser(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        UserResponseDTO deactivated = userService.deactivateUser(id);
        return ResponseEntity.ok(deactivated);
    }
}
