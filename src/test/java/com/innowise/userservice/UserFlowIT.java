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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class UserFlowIT {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/users → 201 created")
    void shouldCreateUser() {
        UserRequestDTO dto = new UserRequestDTO("John", "Doe", LocalDate.of(1990, 1, 1), "john@example.com");

        ResponseEntity<UserResponseDTO> response = rest.postForEntity("/api/users", dto, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    @DisplayName("GET /api/users/{id} → 200")
    void shouldGetUserById() {
        UserResponseDTO created = createUser("John", "Doe", "john@example.com");

        ResponseEntity<UserResponseDTO> response = rest.getForEntity("/api/users/" + created.id(), UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("John");
    }

    @Test
    @DisplayName("GET /api/users/{id} → 404 not found")
    void shouldReturn404ForMissingUser() {
        ResponseEntity<ErrorResponse> response = rest.getForEntity("/api/users/9999", ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PUT /api/users/{id} → 200 updated")
    void shouldUpdateUser() {
        UserResponseDTO created = createUser("John", "Doe", "john@example.com");
        UserRequestDTO updateDTO = new UserRequestDTO("Jane", "Smith", LocalDate.of(1992, 5, 15), "jane@example.com");

        ResponseEntity<UserResponseDTO> response = rest.exchange(
                "/api/users/" + created.id(), HttpMethod.PUT, new HttpEntity<>(updateDTO), UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Jane");
        assertThat(response.getBody().email()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("POST /api/users duplicate email → 409")
    void shouldReturn409OnDuplicateEmail() {
        createUser("John", "Doe", "john@example.com");
        UserRequestDTO duplicate = new UserRequestDTO("Other", "Person", LocalDate.of(1990, 1, 1), "john@example.com");

        ResponseEntity<ErrorResponse> response = rest.postForEntity("/api/users", duplicate, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("PUT /api/users/{id} duplicate email → 409")
    void shouldReturn409OnUpdateDuplicateEmail() {
        createUser("First", "User", "first@example.com");
        UserResponseDTO second = createUser("Second", "User", "second@example.com");
        UserRequestDTO updateDTO = new UserRequestDTO("Second", "User", LocalDate.of(1990, 1, 1), "first@example.com");

        ResponseEntity<ErrorResponse> response = rest.exchange(
                "/api/users/" + second.id(), HttpMethod.PUT, new HttpEntity<>(updateDTO), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} → 204, then GET → 404")
    void shouldSoftDeleteUser() {
        UserResponseDTO created = createUser("John", "Doe", "john@example.com");

        rest.exchange("/api/users/" + created.id() + "/deactivate", HttpMethod.PATCH, null, Void.class);

        ResponseEntity<Void> deleteResponse = rest.exchange(
                "/api/users/" + created.id(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<ErrorResponse> getResponse = rest.getForEntity("/api/users/" + created.id(), ErrorResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/activate → 200, user accessible again")
    void shouldActivateUser() {
        UserResponseDTO created = createUser("John", "Doe", "john@example.com");
        rest.exchange("/api/users/" + created.id() + "/deactivate", HttpMethod.PATCH, null, Void.class);

        ResponseEntity<UserResponseDTO> response = rest.exchange(
                "/api/users/" + created.id() + "/activate", HttpMethod.PATCH, null, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().active()).isTrue();

        ResponseEntity<UserResponseDTO> getResponse = rest.getForEntity("/api/users/" + created.id(), UserResponseDTO.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/deactivate → 200, then GET → 404")
    void shouldDeactivateUser() {
        UserResponseDTO created = createUser("John", "Doe", "john@example.com");

        ResponseEntity<UserResponseDTO> response = rest.exchange(
                "/api/users/" + created.id() + "/deactivate", HttpMethod.PATCH, null, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().active()).isFalse();

        ResponseEntity<ErrorResponse> getResponse = rest.getForEntity("/api/users/" + created.id(), ErrorResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("GET /api/users?name=... → 200 with filtered results")
    void shouldFilterUsers() {
        createUser("John", "Doe", "john@example.com");
        createUser("Jane", "Smith", "jane@example.com");

        ResponseEntity<String> response = rest.getForEntity("/api/users?name=John", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("John");
        assertThat(response.getBody()).doesNotContain("Jane");
    }

    @Test
    @DisplayName("POST /api/users with blank name → 400")
    void shouldReturn400OnInvalidInput() {
        UserRequestDTO invalid = new UserRequestDTO("", "Doe", LocalDate.of(1990, 1, 1), "invalid");

        ResponseEntity<ErrorResponse> response = rest.postForEntity("/api/users", invalid, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("GET /api/users/{id} includes paymentCards list")
    void shouldReturnCardsEmbeddedInUser() {
        UserResponseDTO user = createUser("John", "Doe", "john@example.com");
        addCard(user.id(), "1111111111111111", "John Doe");
        addCard(user.id(), "2222222222222222", "John Doe");

        ResponseEntity<UserResponseDTO> response = rest.getForEntity("/api/users/" + user.id(), UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().paymentCards()).hasSize(2);
        assertThat(response.getBody().paymentCards())
                .extracting(PaymentCardResponseDTO::number)
                .containsExactlyInAnyOrder("1111111111111111", "2222222222222222");
    }

    @Test
    @DisplayName("GET /api/users/{id} includes deactivated cards too")
    void shouldReturnAllCardsIncludingInactive() {
        UserResponseDTO user = createUser("Jane", "Doe", "jane@example.com");
        PaymentCardResponseDTO card = addCard(user.id(), "3333333333333333", "Jane Doe");
        rest.exchange("/api/users/" + user.id() + "/cards/" + card.id() + "/deactivate",
                HttpMethod.PATCH, null, Void.class);

        ResponseEntity<UserResponseDTO> response = rest.getForEntity("/api/users/" + user.id(), UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().paymentCards()).hasSize(1);
        assertThat(response.getBody().paymentCards().getFirst().active()).isFalse();
    }

    private UserResponseDTO createUser(String name, String surname, String email) {
        UserRequestDTO dto = new UserRequestDTO(name, surname, LocalDate.of(1990, 1, 1), email);
        ResponseEntity<UserResponseDTO> response = rest.postForEntity("/api/users", dto, UserResponseDTO.class);
        return response.getBody();
    }

    private PaymentCardResponseDTO addCard(Long userId, String number, String holder) {
        PaymentCardRequestDTO dto = new PaymentCardRequestDTO(number, holder, LocalDate.of(2030, 12, 31));
        return rest.postForEntity("/api/users/" + userId + "/cards", dto, PaymentCardResponseDTO.class).getBody();
    }
}
