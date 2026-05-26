package com.innowise.userservice.service;

import com.innowise.userservice.exception.ActiveCardDeletionException;
import com.innowise.userservice.exception.DuplicateCardNumberException;
import com.innowise.userservice.exception.MaxPaymentCardsLimitException;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;

import java.util.List;

/**
 * Payment card business logic: CRUD, max 5 cards per user.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Maximum 5 cards (active + inactive) per user</li>
 *   <li>Only inactive cards can be hard-deleted</li>
 *   <li>Card number must be globally unique</li>
 * </ul>
 */
public interface PaymentCardService {

    /**
     * Retrieves all cards (active and inactive) for a user.
     *
     * @param userId user ID (must exist and be active)
     * @return list of all payment card DTOs
     * @throws UserNotFoundException if user not found or inactive
     */
    List<PaymentCardResponseDTO> getCardsByUserId(Long userId);

    /**
     * Retrieves an active card by ID.
     *
     * @param id card ID
     * @return payment card DTO
     * @throws PaymentCardNotFoundException if card not found or inactive
     */
    PaymentCardResponseDTO getCardById(Long id);

    /**
     * Adds a new card for a user.
     *
     * @param userId user ID
     * @param dto    card request data
     * @return created card DTO
     * @throws UserNotFoundException          if user not found
     * @throws MaxPaymentCardsLimitException   if user already has 5 cards
     * @throws DuplicateCardNumberException   if card number already exists
     */
    PaymentCardResponseDTO addCard(Long userId, PaymentCardRequestDTO dto);

    /**
     * Updates an active card.
     *
     * @param id  card ID
     * @param dto card request data
     * @return updated card DTO
     * @throws PaymentCardNotFoundException  if card not found or inactive
     * @throws DuplicateCardNumberException  if card number already exists
     */
    PaymentCardResponseDTO updateCard(Long id, PaymentCardRequestDTO dto);

    /**
     * Hard deletes a payment card.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Only inactive cards (active=false) can be deleted</li>
     *   <li>Attempting to delete an active card throws ActiveCardDeletionException</li>
     * </ul>
     *
     * @param id card ID
     * @return deleted card DTO
     * @throws PaymentCardNotFoundException if card not found
     * @throws ActiveCardDeletionException  if card is active
     */
    PaymentCardResponseDTO deleteCard(Long id);

    /**
     * Activates a card (sets active=true).
     *
     * @param id card ID
     * @return activated card DTO
     * @throws PaymentCardNotFoundException if card not found
     */
    PaymentCardResponseDTO activateCard(Long id);

    /**
     * Deactivates a card (sets active=false).
     *
     * @param id card ID
     * @return deactivated card DTO
     * @throws PaymentCardNotFoundException if card not found
     */
    PaymentCardResponseDTO deactivateCard(Long id);
}
