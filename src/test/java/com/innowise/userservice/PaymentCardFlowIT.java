package com.innowise.userservice;

import com.innowise.userservice.model.dto.ErrorResponse;
import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PaymentCardFlowIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
        UserResponseDTO user = createUser("John", "Doe", "john@example.com");
        testUserId = user.id();
    }

    @Test
    @DisplayName("POST /api/users/{userId}/cards → 201")
    void shouldAddCard() {
        PaymentCardRequestDTO dto = cardDTO("1111111111111111", "John Doe");

        ResponseEntity<PaymentCardResponseDTO> response = rest.postForEntity(
                "/api/users/" + testUserId + "/cards", dto, PaymentCardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().number()).isEqualTo("1111111111111111");
        assertThat(response.getBody().userId()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/cards user not found → 404")
    void shouldReturn404WhenAddingCardToMissingUser() {
        PaymentCardRequestDTO dto = cardDTO("1111111111111111", "John Doe");

        ResponseEntity<ErrorResponse> response = rest.postForEntity(
                "/api/users/9999/cards", dto, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/cards exceeds limit → 422")
    void shouldReturn422WhenCardLimitExceeded() {
        for (int i = 1; i <= 5; i++) {
            addCard(testUserId, String.format("%016d", i), "John Doe");
        }

        PaymentCardRequestDTO sixth = cardDTO("9999999999999999", "John Doe");
        ResponseEntity<ErrorResponse> response = rest.postForEntity(
                "/api/users/" + testUserId + "/cards", sixth, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/cards → 200 with card list")
    void shouldGetUserCards() {
        addCard(testUserId, "1111111111111111", "John Doe");
        addCard(testUserId, "2222222222222222", "Jane Doe");

        ResponseEntity<PaymentCardResponseDTO[]> response = rest.getForEntity(
                "/api/users/" + testUserId + "/cards", PaymentCardResponseDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("GET /api/cards/{id} → 200 with card number")
    void shouldGetCardById() {
        PaymentCardResponseDTO card = addCard(testUserId, "1234567812345678", "John Doe");

        ResponseEntity<PaymentCardResponseDTO> response = rest.getForEntity(
                "/api/cards/" + card.id(), PaymentCardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().number()).isEqualTo("1234567812345678");
    }

    @Test
    @DisplayName("GET /api/cards/{id} not found → 404")
    void shouldReturn404ForMissingCard() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("/api/cards/9999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /api/cards/{id} → 200 updated")
    void shouldUpdateCard() {
        PaymentCardResponseDTO card = addCard(testUserId, "1111111111111111", "John Doe");
        PaymentCardRequestDTO updateDTO = cardDTO("2222222222222222", "Updated Name");

        ResponseEntity<PaymentCardResponseDTO> response = rest.exchange(
                "/api/cards/" + card.id(), HttpMethod.PUT, new HttpEntity<>(updateDTO), PaymentCardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().holder()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("DELETE /api/cards/{id} → 204, then GET → 404")
    void shouldSoftDeleteCard() {
        PaymentCardResponseDTO card = addCard(testUserId, "1111111111111111", "John Doe");

        ResponseEntity<Void> deleteResponse = rest.exchange(
                "/api/cards/" + card.id(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ErrorResponse> getResponse = rest.getForEntity("/api/cards/" + card.id(), ErrorResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH /api/cards/{id}/activate → 200, card accessible again")
    void shouldActivateCard() {
        PaymentCardResponseDTO card = addCard(testUserId, "1111111111111111", "John Doe");
        rest.exchange("/api/cards/" + card.id(), HttpMethod.DELETE, null, Void.class);

        ResponseEntity<PaymentCardResponseDTO> response = rest.exchange(
                "/api/cards/" + card.id() + "/activate", HttpMethod.PATCH, null, PaymentCardResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().active()).isTrue();

        ResponseEntity<PaymentCardResponseDTO> getResponse = rest.getForEntity(
                "/api/cards/" + card.id(), PaymentCardResponseDTO.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /api/users/{userId}/cards with invalid data → 400")
    void shouldReturn400OnInvalidCard() {
        PaymentCardRequestDTO invalid = new PaymentCardRequestDTO("abc", "", LocalDate.now().minusDays(1));

        ResponseEntity<ErrorResponse> response = rest.postForEntity(
                "/api/users/" + testUserId + "/cards", invalid, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private UserResponseDTO createUser(String name, String surname, String email) {
        UserRequestDTO dto = new UserRequestDTO(name, surname, LocalDate.of(1990, 1, 1), email);
        return rest.postForEntity("/api/users", dto, UserResponseDTO.class).getBody();
    }

    private PaymentCardResponseDTO addCard(Long userId, String number, String holder) {
        PaymentCardRequestDTO dto = cardDTO(number, holder);
        return rest.postForEntity("/api/users/" + userId + "/cards", dto, PaymentCardResponseDTO.class).getBody();
    }

    private PaymentCardRequestDTO cardDTO(String number, String holder) {
        return new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31));
    }
}
