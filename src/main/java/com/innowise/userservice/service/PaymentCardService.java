package com.innowise.userservice.service;

import com.innowise.userservice.exception.ActiveCardDeletionException;
import com.innowise.userservice.exception.DuplicateCardNumberException;
import com.innowise.userservice.exception.MaxPaymentCardsLimitException;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.PaymentCard;
import com.innowise.userservice.model.User;
import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Payment card business logic: CRUD, max 5 active cards per user. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    private static final int MAX_ACTIVE_CARDS = 5;
    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";
    private static final String PAYMENT_CARD_NOT_FOUND_MSG = "Payment card not found with id: ";

    @Cacheable(value = "userCards", key = "#userId")
    @Transactional(readOnly = true)
    public List<PaymentCardResponseDTO> getCardsByUserId(Long userId) {
        log.debug("Fetching active cards for user {}", userId);
        userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));
        return paymentCardRepository.findByUserIdAndActive(userId, true).stream()
                .map(paymentCardMapper::toResponseDTO)
                .toList();
    }

    @Cacheable(value = "paymentCards", key = "#id")
    @Transactional(readOnly = true)
    public PaymentCardResponseDTO getCardById(Long id) {
        log.debug("Fetching payment card {}", id);
        return paymentCardRepository.findById(id)
                .filter(PaymentCard::isActive)
                .map(paymentCardMapper::toResponseDTO)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
    }

    @CacheEvict(value = "userCards", key = "#userId")
    @Transactional
    public PaymentCardResponseDTO addCard(Long userId, PaymentCardRequestDTO dto) {
        log.info("Adding payment card for user {}", userId);
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        long activeCardsCount = paymentCardRepository.countActiveCardsByUserId(userId);
        if (activeCardsCount >= MAX_ACTIVE_CARDS) {
            log.warn("User {} has reached the maximum limit of {} active payment cards", userId, MAX_ACTIVE_CARDS);
            throw new MaxPaymentCardsLimitException(
                    "User " + userId + " has reached the maximum limit of " + MAX_ACTIVE_CARDS + " active payment cards");
        }

        try {
            PaymentCard card = paymentCardMapper.toEntity(dto);
            card.setActive(true);
            user.addPaymentCard(card);
            PaymentCard saved = paymentCardRepository.saveAndFlush(card);
            log.info("Payment card {} created for user {}", saved.getId(), userId);
            return paymentCardMapper.toResponseDTO(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate card number on add for user {}", userId);
            throw new DuplicateCardNumberException("Card with this number already exists");
        }
    }

    @Caching(
            put  = { @CachePut(value = "paymentCards", key = "#id") },
            evict = { @CacheEvict(value = "userCards", key = "#result.userId") }
    )
    @Transactional
    public PaymentCardResponseDTO updateCard(Long id, PaymentCardRequestDTO dto) {
        log.info("Updating payment card {}", id);
        PaymentCard existing = paymentCardRepository.findById(id)
                .filter(PaymentCard::isActive)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));

        paymentCardMapper.updateEntityFromDTO(dto, existing);
        try {
            paymentCardRepository.saveAndFlush(existing);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate card number on update card {}", id);
            throw new DuplicateCardNumberException("Card with this number already exists");
        }
        return paymentCardMapper.toResponseDTO(existing);
    }

    /**
     * Hard deletes a payment card.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Only inactive cards (active=false) can be deleted</li>
     *   <li>Attempting to delete an active card throws ActiveCardDeletionException</li>
     *   <li>Card is removed from user's collection before deletion to keep JPA state consistent</li>
     * </ul>
     *
     * @param id Payment card ID
     * @throws PaymentCardNotFoundException if card not found
     * @throws ActiveCardDeletionException  if card is active
     */
    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO deleteCard(Long id) {
        log.info("Hard deleting payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
        if (card.isActive()) {
            throw new ActiveCardDeletionException(
                    "Cannot delete active card with id: " + id + ". Deactivate it first.");
        }
        PaymentCardResponseDTO response = paymentCardMapper.toResponseDTO(card);
        card.getUser().removePaymentCard(card);
        paymentCardRepository.delete(card);
        return response;
    }

    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO activateCard(Long id) {
        log.info("Activating payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
        if (!card.isActive()) {
            Long userId = card.getUser().getId();
            userRepository.findByIdWithLock(userId)
                    .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));
            long activeCardsCount = paymentCardRepository.countActiveCardsByUserId(userId);
            if (activeCardsCount >= MAX_ACTIVE_CARDS) {
                log.warn("User {} has reached the maximum limit of {} active payment cards on activation", userId, MAX_ACTIVE_CARDS);
                throw new MaxPaymentCardsLimitException("User " + userId + " has reached the maximum limit of " + MAX_ACTIVE_CARDS + " active payment cards");
            }
            card.setActive(true);
        }
        return paymentCardMapper.toResponseDTO(card);
    }

    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO deactivateCard(Long id) {
        log.info("Deactivating payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
        card.setActive(false);
        return paymentCardMapper.toResponseDTO(card);
    }
}
