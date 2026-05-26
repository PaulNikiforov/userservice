package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.UserDeactivationNotAllowedException;
import com.innowise.userservice.exception.UserDeletionNotAllowedException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.User;
import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.specification.UserSpecification;
import com.innowise.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;
    private final UserMapper userMapper;

    @Cacheable(value = "users", key = "#id")
    @Transactional(readOnly = true)
    @Override
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user {}", id);
        return userRepository.findById(id)
                .filter(User::isActive)
                .map(userMapper::toResponseDTO)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + id));
    }

    @Transactional
    @Override
    public UserResponseDTO createUser(UserRequestDTO dto) {
        log.info("Creating user with email {}", dto.email());
        try {
            User user = userMapper.toEntity(dto);
            user.setActive(true);
            User saved = userRepository.saveAndFlush(user);
            log.info("User {} created with id {}", dto.email(), saved.getId());
            return userMapper.toResponseDTO(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate email on create: {}", dto.email());
            throw new DuplicateEmailException("User with email " + dto.email() + " already exists");
        }
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    @Override
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        log.info("Updating user {}", id);
        User existing = userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + id));

        try {
            userMapper.updateEntityFromDTO(dto, existing);
            userRepository.saveAndFlush(existing);
            return userMapper.toResponseDTO(existing);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate email on update user {}: {}", id, dto.email());
            throw new DuplicateEmailException("User with email " + dto.email() + " already exists");
        }
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    @Override
    public void deleteUser(Long id) {
        log.info("Hard deleting user {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + id));
        if (user.isActive()) {
            throw new UserDeletionNotAllowedException(
                    "Cannot delete active user with id: " + id + ". Deactivate the user first.");
        }
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserResponseDTO> filterUsers(UserFilterDTO filter, Pageable pageable) {
        log.debug("Filtering users with {}", filter);
        Specification<User> spec = UserSpecification.filter(
                filter.name(),
                filter.surname(),
                filter.active()
        );

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toResponseDTO);
    }

    @CachePut(value = "users", key = "#id")
    @Transactional
    @Override
    public UserResponseDTO activateUser(Long id) {
        log.info("Activating user {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + id));
        user.setActive(true);
        return userMapper.toResponseDTO(user);
    }

    @CacheEvict(value = "users", key = "#id")
    @Transactional
    @Override
    public UserResponseDTO deactivateUser(Long id) {
        log.info("Deactivating user {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + id));
        long activeCards = paymentCardRepository.countActiveCardsByUserId(id);
        if (activeCards > 0) {
            log.warn("Cannot deactivate user {} — has {} active card(s)", id, activeCards);
            throw new UserDeactivationNotAllowedException(
                    "Cannot deactivate user with id: " + id + ". User has " + activeCards
                    + " active card(s). Deactivate all cards first.");
        }
        user.setActive(false);
        return userMapper.toResponseDTO(user);
    }
}
