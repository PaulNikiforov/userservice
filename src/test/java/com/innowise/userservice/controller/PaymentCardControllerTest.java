package com.innowise.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.exception.PaymentCardNotFoundException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.service.PaymentCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentCardController.class)
class PaymentCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private PaymentCardService paymentCardService;

    private PaymentCardRequestDTO requestDTO;
    private PaymentCardResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new PaymentCardRequestDTO(
                "1234567812345678",
                "John Doe",
                LocalDate.of(2030, 12, 31)
        );

        responseDTO = new PaymentCardResponseDTO(
                1L,
                "1234567812345678",
                "John Doe",
                LocalDate.of(2030, 12, 31),
                true,
                1L,
                null,
                null
        );
    }

    @Test
    void addCard_Success() throws Exception {
        when(paymentCardService.addCard(eq(1L), any(PaymentCardRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/users/1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.number").value("1234567812345678"));
    }

    @Test
    void addCard_UserNotFound() throws Exception {
        when(paymentCardService.addCard(eq(999L), any(PaymentCardRequestDTO.class)))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(post("/api/users/999/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserCards_Success() throws Exception {
        when(paymentCardService.getCardsByUserId(1L)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/users/1/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("1234567812345678"));
    }

    @Test
    void getUserCards_UserNotFound() throws Exception {
        when(paymentCardService.getCardsByUserId(999L)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/999/cards"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardById_Success() throws Exception {
        when(paymentCardService.getCardById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.holder").value("John Doe"));
    }

    @Test
    void getCardById_NotFound() throws Exception {
        when(paymentCardService.getCardById(999L)).thenThrow(new PaymentCardNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCard_Success() throws Exception {
        when(paymentCardService.updateCard(eq(1L), any(PaymentCardRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/cards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("1234567812345678"));
    }

    @Test
    void updateCard_NotFound() throws Exception {
        when(paymentCardService.updateCard(eq(999L), any(PaymentCardRequestDTO.class)))
                .thenThrow(new PaymentCardNotFoundException("Card not found"));

        mockMvc.perform(put("/api/cards/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCard_Success() throws Exception {
        when(paymentCardService.deleteCard(1L)).thenReturn(responseDTO);

        mockMvc.perform(delete("/api/cards/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void activateCard_Success() throws Exception {
        when(paymentCardService.activateCard(1L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/cards/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deactivateCard_Success() throws Exception {
        when(paymentCardService.deactivateCard(1L)).thenReturn(responseDTO);

        mockMvc.perform(patch("/api/cards/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }
}
