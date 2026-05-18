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
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;

@Entity
@Table(name = "payment_cards")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class PaymentCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_cards_id_seq")
    @SequenceGenerator(name = "payment_cards_id_seq", sequenceName = "payment_cards_id_seq", allocationSize = 50)
    private Long id;

    @BatchSize(size = 50)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "number")
    private String number;

    @Column(name = "holder")
    private String holder;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active")
    private Boolean active = true;

    public PaymentCard(User user, String number, String holder, LocalDate expirationDate) {
        this.user = user;
        this.number = number;
        this.holder = holder;
        this.expirationDate = expirationDate;
    }
}
