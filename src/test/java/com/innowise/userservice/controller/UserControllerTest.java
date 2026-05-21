package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.UserFilterDTO;
import com.innowise.userservice.model.dto.UserRequestDTO;
import com.innowise.userservice.model.dto.UserResponseDTO;
import com.innowise.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserRequestDTO requestDTO;
    private UserResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new UserRequestDTO(
                "John",
                "Doe",
                LocalDate.of(1990, 1, 1),
                "john@example.com"
        );

        responseDTO = new UserResponseDTO(
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
    void createUser_Success() throws Exception {
        when(userService.createUser(any(UserRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void getUserById_NotFound() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterUsers_Success() throws Exception {
        Page<UserResponseDTO> page = new PageImpl<>(List.of(responseDTO));
        when(userService.filterUsers(any(UserFilterDTO.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John"));
    }

    @Test
    void updateUser_Success() throws Exception {
        when(userService.updateUser(eq(1L), any(UserRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void updateUser_NotFound() throws Exception {
        when(userService.updateUser(eq(999L), any(UserRequestDTO.class)))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_NotFound() throws Exception {
        org.mockito.Mockito.doThrow(new UserNotFoundException(999L)).when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateUser_Success() throws Exception {
        when(userService.activateUser(1L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/users/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deactivateUser_Success() throws Exception {
        when(userService.deactivateUser(1L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
