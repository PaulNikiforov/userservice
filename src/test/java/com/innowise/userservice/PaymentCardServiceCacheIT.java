package com.innowise.userservice;

import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentCardServiceCacheIT {

    @Autowired
    private PaymentCardService paymentCardService;

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();

        UserResponseDTO user = userService.createUser(
                new UserRequestDTO("John", "Doe", LocalDate.of(1990, 1, 1), "john@example.com")
        );
        testUserId = user.id();
    }

    @Test
    @DisplayName("Should cache cards list after first retrieval — repository called once")
    void shouldCacheCardsListAfterFirstRetrieval() {
        addTestCard("1111111111111111", "John Doe");

        paymentCardService.getCardsByUserId(testUserId);
        paymentCardService.getCardsByUserId(testUserId);

        verify(paymentCardRepository, times(1)).findByUserIdAndActive(testUserId, true);
    }

    @Test
    @DisplayName("Should cache single card after first retrieval — repository called once")
    void shouldCacheSingleCardAfterFirstRetrieval() {
        PaymentCardResponseDTO card = addTestCard("2222222222222222", "John Doe");

        paymentCardService.getCardById(card.id());
        paymentCardService.getCardById(card.id());

        verify(paymentCardRepository, times(1)).findById(card.id());
    }

    @Test
    @DisplayName("Should evict userCards cache when card is added")
    void shouldEvictUserCardsWhenCardAdded() {
        paymentCardService.getCardsByUserId(testUserId);
        assertThat(getCachedUserCards(testUserId)).isNotNull();

        addTestCard("3333333333333333", "John Doe");

        assertThat(getCachedUserCards(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict only affected user's cards on card update, not other users")
    void shouldEvictOnlyAffectedUserOnCardUpdate() {
        Long userBId = createSecondUser();

        addTestCard("4444444444444444", "John Doe");
        addTestCardForUser(userBId, "5555555555555555", "Jane Smith");

        paymentCardService.getCardsByUserId(testUserId);
        paymentCardService.getCardsByUserId(userBId);
        assertThat(getCachedUserCards(testUserId)).isNotNull();
        assertThat(getCachedUserCards(userBId)).isNotNull();

        PaymentCardResponseDTO cardA = paymentCardService.getCardsByUserId(testUserId).getFirst();
        PaymentCardRequestDTO updateDTO = new PaymentCardRequestDTO("6666666666666666", "Updated", LocalDate.of(2030, 12, 31));
        paymentCardService.updateCard(cardA.id(), updateDTO);

        assertThat(getCachedUserCards(testUserId)).isNull();
        assertThat(getCachedUserCards(userBId)).isNotNull();
    }

    @Test
    @DisplayName("Should throw for inactive card via getCardById")
    void shouldThrowForInactiveCard() {
        PaymentCardResponseDTO card = addTestCard("7777777777777777", "John Doe");
        paymentCardService.deactivateCard(card.id());
        paymentCardService.deleteCard(card.id());
        cacheManager.getCache("paymentCards").clear();

        Long cardId = card.id();
        assertThatThrownBy(() -> paymentCardService.getCardById(cardId))
                .isInstanceOf(PaymentCardNotFoundException.class);
    }

    @Test
    @DisplayName("Should evict caches when card is soft-deleted")
    void shouldEvictCachesWhenCardDeleted() {
        PaymentCardResponseDTO card = addTestCard("8888888888888888", "John Doe");

        paymentCardService.getCardsByUserId(testUserId);

        paymentCardService.deactivateCard(card.id());
        paymentCardService.deleteCard(card.id());

        assertThat(getCachedPaymentCard(card.id())).isNull();
        assertThat(getCachedUserCards(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict caches when card is activated")
    void shouldEvictCachesWhenCardActivated() {
        PaymentCardResponseDTO card = addTestCard("9999999999999999", "John Doe");
        paymentCardService.deactivateCard(card.id());

        paymentCardService.getCardsByUserId(testUserId);

        paymentCardService.activateCard(card.id());

        assertThat(getCachedPaymentCard(card.id())).isNull();
        assertThat(getCachedUserCards(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict caches when card is deactivated")
    void shouldEvictCachesWhenCardDeactivated() {
        PaymentCardResponseDTO card = addTestCard("1212121212121212", "John Doe");

        paymentCardService.getCardsByUserId(testUserId);

        paymentCardService.deactivateCard(card.id());

        assertThat(getCachedPaymentCard(card.id())).isNull();
        assertThat(getCachedUserCards(testUserId)).isNull();
    }

    private PaymentCardResponseDTO addTestCard(String number, String holder) {
        return paymentCardService.addCard(testUserId,
                new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31)));
    }

    private PaymentCardResponseDTO addTestCardForUser(Long userId, String number, String holder) {
        return paymentCardService.addCard(userId,
                new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31)));
    }

    private Long createSecondUser() {
        UserResponseDTO user = userService.createUser(
                new UserRequestDTO("Jane", "Smith", LocalDate.of(1992, 3, 15), "jane@example.com")
        );
        return user.id();
    }

    private List<PaymentCardResponseDTO> getCachedUserCards(Long userId) {
        var cache = cacheManager.getCache("userCards");
        if (cache == null) return null;
        return cache.get(userId, List.class);
    }

    private Object getCachedPaymentCard(Long id) {
        var cache = cacheManager.getCache("paymentCards");
        if (cache == null) return null;
        var wrapper = cache.get(id);
        return wrapper != null ? wrapper.get() : null;
    }
}
