package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.service.PaymentCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for payment card operations.
 *
 * <p>Provides CRUD operations for payment cards plus activate/deactivate functionality.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Payment Cards", description = "Payment card management APIs")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    /**
     * Adds a payment card to a user.
     *
     * @param userId the user ID
     * @param dto the card data
     * @return the created card
     */
    @PostMapping("/users/{userId}/cards")
    @Operation(summary = "Add card to user", description = "Adds a new payment card to a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "422", description = "Maximum cards limit reached")
    })
    public ResponseEntity<PaymentCardResponseDTO> addPaymentCard(
        @Parameter(description = "User ID") @PathVariable Long userId,
        @Parameter(description = "Card data") @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO created = paymentCardService.addCard(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets all payment cards for a user.
     *
     * @param userId the user ID
     * @return list of cards
     */
    @GetMapping("/users/{userId}/cards")
    @Operation(summary = "Get user cards", description = "Retrieves all active payment cards for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<PaymentCardResponseDTO>> getUserCards(
        @Parameter(description = "User ID") @PathVariable Long userId
    ) {
        List<PaymentCardResponseDTO> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    /**
     * Gets a payment card by ID.
     *
     * @param id the card ID
     * @return the card data
     */
    @GetMapping("/cards/{id}")
    @Operation(summary = "Get card by ID", description = "Retrieves a payment card by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<PaymentCardResponseDTO> getCardById(
        @Parameter(description = "Card ID") @PathVariable Long id
    ) {
        PaymentCardResponseDTO card = paymentCardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    /**
     * Update a payment card.
     *
     * @param id the card ID
     * @param dto the updated card data
     * @return the updated card
     */
    @PutMapping("/cards/{id}")
    @Operation(summary = "Update card", description = "Updates an existing payment card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<PaymentCardResponseDTO> updateCard(
        @Parameter(description = "Card ID") @PathVariable Long id,
        @Parameter(description = "Updated card data") @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO updated = paymentCardService.updateCard(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deactivates a payment card (soft delete).
     *
     * @param id the card ID
     * @return 204 status
     */
    @DeleteMapping("/cards/{id}")
    @Operation(summary = "Delete card", description = "Soft deletes a payment card by setting active=false")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> deleteCard(
        @Parameter(description = "Card ID") @PathVariable Long id
    ) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates a payment card.
     *
     * @param id the card ID
     * @return the activated card
     */
    @PatchMapping("/cards/{id}/activate")
    @Operation(summary = "Activate card", description = "Activates a deactivated payment card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card activated successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<PaymentCardResponseDTO> activateCard(
        @Parameter(description = "Card ID") @PathVariable Long id
    ) {
        PaymentCardResponseDTO activated = paymentCardService.activateCard(id);
        return ResponseEntity.ok(activated);
    }
}
