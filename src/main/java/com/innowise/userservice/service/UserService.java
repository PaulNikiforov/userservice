package com.innowise.userservice.service;

import com.innowise.userservice.exception.DuplicateEmailException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.User;
import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.repository.specification.UserSpecification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return userMapper.toResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO createUser(@Valid UserRequestDTO dto) {
        try {
            User user = userMapper.toEntity(dto);
            user.setActive(true);
            User saved = userRepository.save(user);
            return userMapper.toResponseDTO(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException("User with email " + dto.email() + " already exists");
        }
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, @Valid UserRequestDTO dto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        try {
            userMapper.updateEntityFromDTO(dto, existing);
            return userMapper.toResponseDTO(existing);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException("User with email " + dto.email() + " already exists");
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        user.setActive(false);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> filterUsers(UserFilterDTO filter, Pageable pageable) {
        Specification<User> spec = UserSpecification.filter(
                filter.name(),
                filter.surname(),
                filter.email(),
                filter.active()
        );

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toResponseDTO);
    }
}
