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
    @DisplayName("Should evict user cache when card is added")
    void shouldEvictUserCacheWhenCardAdded() {
        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId)).isNotNull();

        addTestCard("3333333333333333", "John Doe");

        assertThat(getCachedUser(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict user cache when card is updated")
    void shouldEvictUserCacheWhenCardUpdated() {
        PaymentCardResponseDTO card = addTestCard("4444444444444444", "John Doe");
        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId)).isNotNull();

        paymentCardService.updateCard(card.id(),
                new PaymentCardRequestDTO("4444444444444444", "Updated Name", LocalDate.of(2030, 12, 31)));

        assertThat(getCachedUser(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict user cache when card is deleted")
    void shouldEvictUserCacheWhenCardDeleted() {
        PaymentCardResponseDTO card = addTestCard("8888888888888888", "John Doe");
        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId)).isNotNull();

        paymentCardService.deactivateCard(card.id());
        paymentCardService.deleteCard(card.id());

        assertThat(getCachedUser(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict user cache when card is activated")
    void shouldEvictUserCacheWhenCardActivated() {
        PaymentCardResponseDTO card = addTestCard("9999999999999999", "John Doe");
        paymentCardService.deactivateCard(card.id());

        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId)).isNotNull();

        paymentCardService.activateCard(card.id());

        assertThat(getCachedUser(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict user cache when card is deactivated")
    void shouldEvictUserCacheWhenCardDeactivated() {
        PaymentCardResponseDTO card = addTestCard("1212121212121212", "John Doe");
        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId)).isNotNull();

        paymentCardService.deactivateCard(card.id());

        assertThat(getCachedUser(testUserId)).isNull();
    }

    @Test
    @DisplayName("Should evict only affected user's cache on card update")
    void shouldEvictOnlyAffectedUserCacheOnCardUpdate() {
        Long userBId = createSecondUser();
        PaymentCardResponseDTO cardA = addTestCard("4444444444444444", "John Doe");
        addTestCardForUser(userBId, "5555555555555555", "Jane Smith");

        userService.getUserById(testUserId);
        userService.getUserById(userBId);
        assertThat(getCachedUser(testUserId)).isNotNull();
        assertThat(getCachedUser(userBId)).isNotNull();

        paymentCardService.updateCard(cardA.id(),
                new PaymentCardRequestDTO("6666666666666666", "Updated", LocalDate.of(2030, 12, 31)));

        assertThat(getCachedUser(testUserId)).isNull();
        assertThat(getCachedUser(userBId)).isNotNull();
    }

    @Test
    @DisplayName("After cache evict, re-fetched user contains updated card data")
    void shouldReflectCardChangesAfterCacheEvict() {
        userService.getUserById(testUserId);
        assertThat(getCachedUser(testUserId).paymentCards()).isEmpty();

        addTestCard("1111111111111111", "John Doe");

        // cache evicted — next call re-fetches from DB
        UserResponseDTO updated = userService.getUserById(testUserId);
        assertThat(updated.paymentCards()).hasSize(1);
        assertThat(updated.paymentCards().getFirst().number()).isEqualTo("1111111111111111");
    }

    @Test
    @DisplayName("getCardById returns card without caching — repository always called")
    void getCardByIdAlwaysHitsRepository() {
        PaymentCardResponseDTO card = addTestCard("2222222222222222", "John Doe");

        paymentCardService.getCardById(card.id());
        paymentCardService.getCardById(card.id());

        verify(paymentCardRepository, times(2)).findById(card.id());
    }

    @Test
    @DisplayName("getCardById throws for deleted card")
    void shouldThrowForDeletedCard() {
        PaymentCardResponseDTO card = addTestCard("7777777777777777", "John Doe");
        paymentCardService.deactivateCard(card.id());
        paymentCardService.deleteCard(card.id());

        Long cardId = card.id();
        assertThatThrownBy(() -> paymentCardService.getCardById(cardId))
                .isInstanceOf(PaymentCardNotFoundException.class);
    }

    // --- helpers ---

    private PaymentCardResponseDTO addTestCard(String number, String holder) {
        return paymentCardService.addCard(testUserId,
                new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31)));
    }

    private PaymentCardResponseDTO addTestCardForUser(Long userId, String number, String holder) {
        return paymentCardService.addCard(userId,
                new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31)));
    }

    private Long createSecondUser() {
        return userService.createUser(
                new UserRequestDTO("Jane", "Smith", LocalDate.of(1992, 3, 15), "jane@example.com")
        ).id();
    }

    private UserResponseDTO getCachedUser(Long userId) {
        var cache = cacheManager.getCache("users");
        if (cache == null) return null;
        return cache.get(userId, UserResponseDTO.class);
    }
}
