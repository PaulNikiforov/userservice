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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private PaymentCardService paymentCardService;

    private User testUser;
    private PaymentCard testCard;
    private PaymentCardRequestDTO testRequestDTO;
    private PaymentCardResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setEmail("john@example.com");
        testUser.setActive(true);

        testCard = new PaymentCard();
        testCard.setId(1L);
        testCard.setUser(testUser);
        testCard.setNumber("1234567812345678");
        testCard.setHolder("John Doe");
        testCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        testCard.setActive(true);

        testRequestDTO = new PaymentCardRequestDTO(
                "1234567812345678",
                "John Doe",
                LocalDate.of(2025, 12, 31)
        );

        testResponseDTO = new PaymentCardResponseDTO(
                1L,
                "**** **** **** 5678",
                "John Doe",
                LocalDate.of(2025, 12, 31),
                true,
                1L,
                null,
                null
        );
    }

    @Test
    void testGetCardById_Success() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(paymentCardMapper.toResponseDTO(testCard)).thenReturn(testResponseDTO);

        PaymentCardResponseDTO result = paymentCardService.getCardById(1L);

        assertNotNull(result);
        assertEquals("**** **** **** 5678", result.number());
        assertEquals("John Doe", result.holder());

        verify(paymentCardRepository, times(1)).findById(1L);
        verify(paymentCardMapper, times(1)).toResponseDTO(testCard);
    }

    @Test
    void testGetCardById_NotFound() {
        when(paymentCardRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PaymentCardNotFoundException.class, () -> paymentCardService.getCardById(999L));

        verify(paymentCardRepository, times(1)).findById(999L);
        verify(paymentCardMapper, never()).toResponseDTO(any());
    }

    @Test
    void testAddCard_Success() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
        when(paymentCardRepository.countActiveCardsByUserId(1L)).thenReturn(2L);
        when(paymentCardMapper.toEntity(testRequestDTO)).thenReturn(testCard);
        when(paymentCardRepository.save(any(PaymentCard.class))).thenReturn(testCard);
        when(paymentCardMapper.toResponseDTO(testCard)).thenReturn(testResponseDTO);

        PaymentCardResponseDTO result = paymentCardService.addCard(1L, testRequestDTO);

        assertNotNull(result);
        assertEquals("**** **** **** 5678", result.number());

        verify(userRepository, times(1)).findByIdWithLock(1L);
        verify(paymentCardRepository, times(1)).countActiveCardsByUserId(1L);
        verify(paymentCardMapper, times(1)).toEntity(testRequestDTO);
        verify(paymentCardRepository, times(1)).save(any(PaymentCard.class));
    }

    @Test
    void testAddCard_UserNotFound() {
        when(userRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> paymentCardService.addCard(999L, testRequestDTO));

        verify(userRepository, times(1)).findByIdWithLock(999L);
        verify(paymentCardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void testAddCard_MaxLimitReached() {
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testUser));
        when(paymentCardRepository.countActiveCardsByUserId(1L)).thenReturn(5L); // Max limit

        assertThrows(MaxPaymentCardsLimitException.class, () -> paymentCardService.addCard(1L, testRequestDTO));

        verify(userRepository, times(1)).findByIdWithLock(1L);
        verify(paymentCardRepository, times(1)).countActiveCardsByUserId(1L);
        verify(paymentCardMapper, never()).toEntity(any());
        verify(paymentCardRepository, never()).save(any(PaymentCard.class));
    }

    @Test
    void testUpdateCard_Success() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(paymentCardMapper.toResponseDTO(testCard)).thenReturn(testResponseDTO);

        PaymentCardResponseDTO result = paymentCardService.updateCard(1L, testRequestDTO);

        assertNotNull(result);
        assertEquals("**** **** **** 5678", result.number());

        verify(paymentCardRepository, times(1)).findById(1L);
        verify(paymentCardMapper, times(1)).updateEntityFromDTO(testRequestDTO, testCard);
    }

    @Test
    void testDeleteCard_Success() {
        when(paymentCardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(paymentCardMapper.toResponseDTO(testCard)).thenReturn(testResponseDTO);

        PaymentCardResponseDTO result = paymentCardService.deleteCard(1L);

        assertFalse(testCard.isActive());
        assertNotNull(result);

        verify(paymentCardRepository, times(1)).findById(1L);
    }
}
