package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.exception.UserDeletionNotAllowedException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRequestDTO testRequestDTO;
    private UserResponseDTO testResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setEmail("john@example.com");
        testUser.setActive(true);

        testRequestDTO = new UserRequestDTO(
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john@example.com"
        );

        testResponseDTO = new UserResponseDTO(
                1L,
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john@example.com",
                true,
                null,
                null
        );
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        UserResponseDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("John", result.name());
        assertEquals("john@example.com", result.email());

        verify(userRepository, times(1)).findById(1L);
        verify(userMapper, times(1)).toResponseDTO(testUser);
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(999L));

        verify(userRepository, times(1)).findById(999L);
        verify(userMapper, never()).toResponseDTO(any());
    }

    @Test
    void getUserById_InactiveUser_ThrowsException() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(1L));

        verify(userMapper, never()).toResponseDTO(any());
    }

    @Test
    void createUser_Success() {
        when(userMapper.toEntity(testRequestDTO)).thenReturn(testUser);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        UserResponseDTO result = userService.createUser(testRequestDTO);

        assertNotNull(result);
        assertEquals("John", result.name());

        verify(userMapper, times(1)).toEntity(testRequestDTO);
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        when(userMapper.toEntity(testRequestDTO)).thenReturn(testUser);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateEmailException.class, () -> userService.createUser(testRequestDTO));

        verify(userMapper, times(1)).toEntity(testRequestDTO);
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        UserResponseDTO result = userService.updateUser(1L, testRequestDTO);

        assertNotNull(result);
        assertEquals("John", result.name());

        verify(userRepository, times(1)).findById(1L);
        verify(userMapper, times(1)).updateEntityFromDTO(testRequestDTO, testUser);
    }

    @Test
    void deleteUser_Success() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void deleteUser_ActiveUser_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(UserDeletionNotAllowedException.class, () -> userService.deleteUser(1L));

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void filterUsers_Success() {
        UserFilterDTO filter = new UserFilterDTO("John", null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        Page<UserResponseDTO> result = userService.filterUsers(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().getFirst().name());

        verify(userRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void updateUser_InactiveUser_ThrowsException() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(1L, testRequestDTO));

        verify(userMapper, never()).updateEntityFromDTO(any(), any());
    }

    @Test
    void activateUser_Success() {
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        UserResponseDTO result = userService.activateUser(1L);

        assertTrue(testUser.isActive());
        assertNotNull(result);

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void deactivateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentCardRepository.countActiveCardsByUserId(1L)).thenReturn(0L);
        when(userMapper.toResponseDTO(testUser)).thenReturn(testResponseDTO);

        UserResponseDTO result = userService.deactivateUser(1L);

        assertFalse(testUser.isActive());
        assertNotNull(result);

        verify(userRepository, times(1)).findById(1L);
        verify(paymentCardRepository, times(1)).countActiveCardsByUserId(1L);
    }

    @Test
    void updateUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(999L, testRequestDTO));
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(1L, testRequestDTO));
    }

    @Test
    void deleteUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(999L));
    }

    @Test
    void activateUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.activateUser(999L));
    }

    @Test
    void deactivateUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deactivateUser(999L));
    }
}
