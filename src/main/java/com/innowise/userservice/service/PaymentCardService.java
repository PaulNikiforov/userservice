package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.exception.MaxPaymentCardsLimitException;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.PaymentCard;
import com.innowise.userservice.model.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    private static final int MAX_ACTIVE_CARDS = 5;

    @Transactional(readOnly = true)
    public PaymentCardResponseDTO getPaymentCardById(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        return paymentCardMapper.toResponseDTO(card);
    }

    @Transactional
    public PaymentCardResponseDTO createPaymentCard(Long userId, @Valid PaymentCardRequestDTO dto) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new com.innowise.userservice.exception.UserNotFoundException("User not found with id: " + userId));

        long activeCardsCount = paymentCardRepository.countActiveCardsByUserId(userId);
        if (activeCardsCount >= MAX_ACTIVE_CARDS) {
            throw new MaxPaymentCardsLimitException("User " + userId + " has reached the maximum limit of " + MAX_ACTIVE_CARDS + " active payment cards");
        }

        PaymentCard card = paymentCardMapper.toEntity(dto);
        card.setUser(user);
        card.setActive(true);
        PaymentCard saved = paymentCardRepository.save(card);
        return paymentCardMapper.toResponseDTO(saved);
    }

    @Transactional
    public PaymentCardResponseDTO updatePaymentCard(Long id, @Valid PaymentCardRequestDTO dto) {
        PaymentCard existing = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));

        paymentCardMapper.updateEntityFromDTO(dto, existing);
        return paymentCardMapper.toResponseDTO(existing);
    }

    @Transactional
    public void deletePaymentCard(Long id) {
        PaymentCard card = paymentCardRepository.findById(id)
                .orElseThrow(() -> new PaymentCardNotFoundException("Payment card not found with id: " + id));
        card.setActive(false);
    }
}
