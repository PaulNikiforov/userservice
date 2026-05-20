package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.PaymentCardRequestDTO;
import com.innowise.userservice.model.dto.PaymentCardResponseDTO;
import com.innowise.userservice.service.PaymentCardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for payment card management. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Payment Cards", description = "Payment card management APIs")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping("/users/{userId}/cards")
    public ResponseEntity<PaymentCardResponseDTO> addPaymentCard(
        @PathVariable Long userId,
        @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO created = paymentCardService.addCard(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<List<PaymentCardResponseDTO>> getUserCards(
        @PathVariable Long userId
    ) {
        List<PaymentCardResponseDTO> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDTO> getCardById(
        @PathVariable Long id
    ) {
        PaymentCardResponseDTO card = paymentCardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/cards/{id}")
    public ResponseEntity<PaymentCardResponseDTO> updateCard(
        @PathVariable Long id,
        @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO updated = paymentCardService.updateCard(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> deleteCard(
        @PathVariable Long id
    ) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/cards/{id}/activate")
    public ResponseEntity<PaymentCardResponseDTO> activateCard(
        @PathVariable Long id
    ) {
        PaymentCardResponseDTO activated = paymentCardService.activateCard(id);
        return ResponseEntity.ok(activated);
    }
}
