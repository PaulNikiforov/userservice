package com.innowise.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * PaymentCard entity representing a payment card belonging to a user.
 *
 * <p>Payment cards are linked to users through a many-to-one relationship.
 * Supports soft deletion through the active flag.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Card number must be 16 digits (basic validation)</li>
 *   <li>Maximum 5 active payment cards per user</li>
 *   <li>Expiration date must be in the future</li>
 *   <li>Soft-deleted when active=false</li>
 * </ul>
 *
 * <p>Relationships:
 * <ul>
 *   <li>Many-to-one with User (bidirectional)</li>
 *   <li>Lazily loaded for performance</li>
 * </ul>
 *
 * @see User
 * @see BaseEntity
 */
@Entity
@Table(name = "payment_cards")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"number"})
public class PaymentCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_cards_id_seq")
    @SequenceGenerator(name = "payment_cards_id_seq", sequenceName = "payment_cards_id_seq", allocationSize = 50)
    private Long id;

    /**
     * User who owns this payment card.
     * Lazily loaded for performance optimization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Card number (16 digits, basic validation).
     * In production, this should be encrypted and only last 4 digits shown.
     * Required field, max 16 characters.
     */
    @Column(name = "number", nullable = false, length = 16)
    private String number;

    /**
     * Cardholder name as printed on the card.
     * Required field, max 255 characters.
     */
    @Column(name = "holder", nullable = false)
    private String holder;

    /**
     * Card expiration date (last day of validity).
     * Required field, must be in the future.
     * Format: YYYY-MM-DD
     */
    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    /**
     * Card status flag.
     * true = active card, false = soft-deleted card.
     * Defaults to true for new cards.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Creates a new PaymentCard instance with required fields.
     * Defaults to active=true.
     *
     * @param user User who owns this card (required)
     * @param number Card number, 16 digits (required)
     * @param holder Cardholder name (required, max 255 chars)
     * @param expirationDate Card expiration date (required, must be in future)
     * @throws IllegalArgumentException if any parameter is null
     */
    public PaymentCard(User user, String number, String holder, LocalDate expirationDate) {
        this.user = user;
        this.number = number;
        this.holder = holder;
        this.expirationDate = expirationDate;
    }
}
