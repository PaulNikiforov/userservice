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

/** User entity. Supports soft deletion and has up to 5 active payment cards. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"email"})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "surname", nullable = false, length = 100)
    private String surname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentCard> paymentCards = new ArrayList<>();

    public User(String name, String surname, LocalDate birthDate, String email) {
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.email = email;
    }

    public void addPaymentCard(PaymentCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Payment card cannot be null");
        }
        paymentCards.add(card);
        card.setUser(this);
    }

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
