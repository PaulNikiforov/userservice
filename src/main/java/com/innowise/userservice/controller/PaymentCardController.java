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

@RestController
@RequestMapping("/api/users/{userId}/cards")
@RequiredArgsConstructor
@Tag(name = "Payment Cards", description = "Payment card management APIs")
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @PostMapping
    public ResponseEntity<PaymentCardResponseDTO> addPaymentCard(
        @PathVariable Long userId,
        @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO created = paymentCardService.addCard(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PaymentCardResponseDTO>> getUserCards(
        @PathVariable Long userId
    ) {
        List<PaymentCardResponseDTO> cards = paymentCardService.getCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDTO> getCardById(
        @PathVariable Long userId,
        @PathVariable Long id
    ) {
        PaymentCardResponseDTO card = paymentCardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentCardResponseDTO> updateCard(
        @PathVariable Long userId,
        @PathVariable Long id,
        @Valid @RequestBody PaymentCardRequestDTO dto
    ) {
        PaymentCardResponseDTO updated = paymentCardService.updateCard(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
        @PathVariable Long userId,
        @PathVariable Long id
    ) {
        paymentCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<PaymentCardResponseDTO> activateCard(
        @PathVariable Long userId,
        @PathVariable Long id
    ) {
        PaymentCardResponseDTO activated = paymentCardService.activateCard(id);
        return ResponseEntity.ok(activated);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<PaymentCardResponseDTO> deactivateCard(
        @PathVariable Long userId,
        @PathVariable Long id
    ) {
        PaymentCardResponseDTO deactivated = paymentCardService.deactivateCard(id);
        return ResponseEntity.ok(deactivated);
    }
}
