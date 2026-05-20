package com.innowise.userservice;

import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserServiceCacheIT {

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should cache user after first retrieval by ID")
    void shouldCacheUserAfterFirstRetrieval() {
        UserResponseDTO created = createTestUser("John", "Doe", "john@example.com");

        assertThat(getCachedUser(created.id())).isNull();

        UserResponseDTO firstCall = userService.getUserById(created.id());
        assertThat(firstCall).isNotNull();

        assertThat(getCachedUser(created.id())).isNotNull();
    }

    @Test
    @DisplayName("Should serve from cache on second call — repository called only once")
    void shouldReturnCachedUserOnSecondCall() {
        UserResponseDTO created = createTestUser("Jane", "Doe", "jane@example.com");

        userService.getUserById(created.id());
        userService.getUserById(created.id());

        verify(userRepository, times(1)).findById(created.id());
    }

    @Test
    @DisplayName("Should update cache when user is updated via @CachePut")
    void shouldUpdateCacheWhenUserUpdated() {
        UserResponseDTO created = createTestUser("Bob", "Smith", "bob@example.com");

        userService.getUserById(created.id());

        UserRequestDTO updateDTO = new UserRequestDTO("Robert", "Smith",
                LocalDate.of(1990, 1, 1), "robert@example.com");
        UserResponseDTO updated = userService.updateUser(created.id(), updateDTO);

        assertThat(getCachedUser(created.id())).isNotNull();
        assertThat(updated.name()).isEqualTo("Robert");
        assertThat(updated.email()).isEqualTo("robert@example.com");
    }

    @Test
    @DisplayName("Should evict cache when user is soft-deleted")
    void shouldEvictCacheWhenUserDeleted() {
        UserResponseDTO created = createTestUser("Alice", "Brown", "alice@example.com");

        userService.getUserById(created.id());
        assertThat(getCachedUser(created.id())).isNotNull();

        userService.deleteUser(created.id());

        assertThat(getCachedUser(created.id())).isNull();

        long userId = created.id();

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should update cache when user is activated via @CachePut")
    void shouldUpdateCacheWhenUserActivated() {
        UserResponseDTO created = createTestUser("Tom", "Wilson", "tom@example.com");
        userService.deleteUser(created.id());

        UserResponseDTO activated = userService.activateUser(created.id());

        assertThat(activated.active()).isTrue();
        assertThat(getCachedUser(created.id())).isNotNull();
    }

    @Test
    @DisplayName("Should evict cache when user is deactivated via @CacheEvict")
    void shouldEvictCacheWhenUserDeactivated() {
        UserResponseDTO created = createTestUser("Eva", "Green", "eva@example.com");

        UserResponseDTO deactivated = userService.deactivateUser(created.id());

        assertThat(deactivated.active()).isFalse();
        assertThat(getCachedUser(created.id())).isNull();
    }

    @Test
    @DisplayName("Should not cache filtered users (dynamic query)")
    void shouldNotCacheFilteredUsers() {
        createTestUser("User1", "Test", "user1@test.com");
        createTestUser("User2", "Test", "user2@test.com");

        userService.filterUsers(
                new UserFilterDTO(null, null, true),
                PageRequest.of(0, 10)
        );

        var cache = cacheManager.getCache("users");
        assertThat(cache).isNotNull();
        assertThat(cache.get("user1@test.com")).isNull();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for soft-deleted user via getUserById")
    void shouldThrowForSoftDeletedUser() {
        UserResponseDTO created = createTestUser("Deleted", "User", "deleted@example.com");
        userService.deleteUser(created.id());
        Optional.ofNullable(cacheManager.getCache("users")).ifPresent(Cache::clear);

        Long userId = created.id();

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    private UserResponseDTO createTestUser(String name, String surname, String email) {
        UserRequestDTO dto = new UserRequestDTO(name, surname, LocalDate.of(1990, 1, 1), email);
        return userService.createUser(dto);
    }

    private UserResponseDTO getCachedUser(Long id) {
        var cache = cacheManager.getCache("users");
        if (cache == null) return null;
        return cache.get(id, UserResponseDTO.class);
    }
}
