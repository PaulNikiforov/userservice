package com.innowise.userservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity representing a registered user in the system.
 *
 * <p>Users can have multiple payment cards (maximum 5 active cards).
 * Supports soft deletion through the active flag.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Email must be unique</li>
 *   <li>Maximum 5 active payment cards per user</li>
 *   <li>Users are soft-deleted (active=false)</li>
 *   <li>Birthdate must be in the past (user must be 18+ years old)</li>
 * </ul>
 *
 * <p>Relationships:
 * <ul>
 *   <li>One-to-many with PaymentCard (bidirectional)</li>
 *   <li>Cascade operations: ALL (create, update, delete)</li>
 *   <li>Orphan removal enabled for payment cards</li>
 * </ul>
 *
 * @see PaymentCard
 * @see BaseEntity
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 50)
    private Long id;

    /**
     * User's first name.
     * Required field, max 100 characters.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * User's last name/surname.
     * Required field, max 100 characters.
     */
    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    /**
     * User's date of birth.
     * Required field, must be in the past (user must be 18+ years old).
     */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /**
     * User's unique email address used for login.
     * Required field, max 100 characters, must be unique across all users.
     * Stored in lowercase for consistency.
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * User account status flag.
     * true = active account, false = soft-deleted account.
     * Defaults to true for new users.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * User's payment cards (bidirectional relationship).
     * Lazily loaded with batch size optimization.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Maximum 5 active cards per user</li>
     *   <li>Cascade all operations (create, update, delete)</li>
     *   <li>Orphan removal enabled</li>
     * </ul>
     */
    @BatchSize(size = 50)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentCard> paymentCards = new ArrayList<>();

    /**
     * Creates a new User instance with required fields.
     * Defaults to active=true.
     *
     * @param name User's first name (required, max 100 chars)
     * @param surname User's last name (required, max 100 chars)
     * @param birthDate User's date of birth (must be in the past, 18+ years old)
     * @param email User's unique email address (required, valid email format)
     */
    public User(String name, String surname, LocalDate birthDate, String email) {
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.email = email;
    }

    /**
     * Adds a payment card to this user's card collection.
     * Automatically sets up bidirectional relationship.
     *
     * @param card PaymentCard to add (required)
     * @throws IllegalArgumentException if card is null
     */
    public void addPaymentCard(PaymentCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Payment card cannot be null");
        }
        paymentCards.add(card);
        card.setUser(this);
    }

    /**
     * Removes a payment card from this user's card collection.
     * Automatically breaks bidirectional relationship.
     *
     * @param card PaymentCard to remove (required)
     * @return true if card was found and removed, false otherwise
     * @throws IllegalArgumentException if card is null
     */
    public boolean removePaymentCard(PaymentCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Payment card cannot be null");
        }

        // Use removeIf to handle both persisted (with id) and non-persisted (without id) cards
        boolean removed = paymentCards.removeIf(c -> {
            if (c.getId() != null) {
                return c.getId().equals(card.getId());
            } else {
                // For non-persisted cards, compare by reference
                return c == card;
            }
        });

        if (removed) {
            card.setUser(null);
        }
        return removed;
    }
}