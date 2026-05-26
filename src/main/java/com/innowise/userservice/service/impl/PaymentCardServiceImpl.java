package com.innowise.userservice.service.impl;

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
import com.innowise.userservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    private static final int MAX_CARDS_PER_USER = 5;
    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";
    private static final String PAYMENT_CARD_NOT_FOUND_MSG = "Payment card not found with id: ";

    @Transactional(readOnly = true)
    @Override
    public List<PaymentCardResponseDTO> getCardsByUserId(Long userId) {
        log.debug("Fetching all cards for user {}", userId);
        userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));
        return paymentCardRepository.findByUserId(userId).stream()
                .map(paymentCardMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PaymentCardResponseDTO getCardById(Long id) {
        log.debug("Fetching payment card {}", id);
        return paymentCardRepository.findById(id)
                .filter(PaymentCard::isActive)
                .map(paymentCardMapper::toResponseDTO)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    @Override
    public PaymentCardResponseDTO addCard(Long userId, PaymentCardRequestDTO dto) {
        log.info("Adding payment card for user {}", userId);
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        long cardsCount = paymentCardRepository.countByUserId(userId);
        if (cardsCount >= MAX_CARDS_PER_USER) {
            log.warn("User {} has reached the maximum limit of {} payment cards", userId, MAX_CARDS_PER_USER);
            throw new MaxPaymentCardsLimitException(
                    "User " + userId + " has reached the maximum limit of " + MAX_CARDS_PER_USER + " payment cards");
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

    @CacheEvict(value = "users", key = "#result.userId")
    @Transactional
    @Override
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

    @CacheEvict(value = "users", key = "#result.userId")
    @Transactional
    @Override
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

    @CacheEvict(value = "users", key = "#result.userId")
    @Transactional
    @Override
    public PaymentCardResponseDTO activateCard(Long id) {
        log.info("Activating payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
        if (!card.isActive()) {
            card.setActive(true);
        }
        return paymentCardMapper.toResponseDTO(card);
    }

    @CacheEvict(value = "users", key = "#result.userId")
    @Transactional
    @Override
    public PaymentCardResponseDTO deactivateCard(Long id) {
        log.info("Deactivating payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException(PAYMENT_CARD_NOT_FOUND_MSG + id));
        card.setActive(false);
        return paymentCardMapper.toResponseDTO(card);
    }
}
