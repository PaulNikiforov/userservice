package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.exception.MaxPaymentCardsLimitException;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.PaymentCard;
import com.innowise.userservice.model.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for payment card business logic.
 *
 * <p>Manages CRUD operations for payment cards with business rule enforcement:
 * maximum 5 active cards per user, soft deletion, and activation/deactivation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    private static final int MAX_ACTIVE_CARDS = 5;

    @Cacheable(value = "userCards", key = "#userId")
    @Transactional(readOnly = true)
    public List<PaymentCardResponseDTO> getCardsByUserId(Long userId) {
        log.debug("Fetching active cards for user {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
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
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
    }

    @CacheEvict(value = "userCards", key = "#userId")
    @Transactional
    public PaymentCardResponseDTO addCard(Long userId, @Valid PaymentCardRequestDTO dto) {
        log.info("Adding payment card for user {}", userId);
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        long activeCardsCount = paymentCardRepository.countActiveCardsByUserId(userId);
        if (activeCardsCount >= MAX_ACTIVE_CARDS) {
            log.warn("User {} has reached the maximum limit of {} active payment cards", userId, MAX_ACTIVE_CARDS);
            throw new MaxPaymentCardsLimitException("User " + userId + " has reached the maximum limit of " + MAX_ACTIVE_CARDS + " active payment cards");
        }

        PaymentCard card = paymentCardMapper.toEntity(dto);
        card.setActive(true);
        user.addPaymentCard(card);
        PaymentCard saved = paymentCardRepository.save(card);
        log.info("Payment card {} created for user {}", saved.getId(), userId);
        return paymentCardMapper.toResponseDTO(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO updateCard(Long id, @Valid PaymentCardRequestDTO dto) {
        log.info("Updating payment card {}", id);
        PaymentCard existing = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));

        paymentCardMapper.updateEntityFromDTO(dto, existing);
        paymentCardRepository.saveAndFlush(existing);
        return paymentCardMapper.toResponseDTO(existing);
    }

    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO deleteCard(Long id) {
        log.info("Soft deleting payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        card.setActive(false);
        return paymentCardMapper.toResponseDTO(card);
    }

    @Caching(evict = {
            @CacheEvict(value = "paymentCards", key = "#id"),
            @CacheEvict(value = "userCards", key = "#result.userId")
    })
    @Transactional
    public PaymentCardResponseDTO activateCard(Long id) {
        log.info("Activating payment card {}", id);
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        card.setActive(true);
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
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        card.setActive(false);
        return paymentCardMapper.toResponseDTO(card);
    }
}
