package com.innowise.userservice.repository;

import com.innowise.userservice.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends
        JpaRepository<PaymentCard, Long> {

    List<PaymentCard> findByUserIdAndActive(Long userId, Boolean active);

    @Query(value = """
        SELECT COUNT(*)
        FROM payment_cards
        WHERE user_id = :userId AND active = true
        """, nativeQuery = true)
    long countActiveCardsByUserId(@Param("userId") Long userId);
}
