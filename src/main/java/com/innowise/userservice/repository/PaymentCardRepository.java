package com.innowise.userservice.repository;

import com.innowise.userservice.model.PaymentCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends
        JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    /**
     * Finds all payment cards of a user filtered by active status.
     *
     * @param userId the ID of the user
     * @param active filter by active status
     * @return list of matching payment cards
     */
    List<PaymentCard> findByUserIdAndActive(Long userId, Boolean active);

    /**
     * Counts active payment cards for a specific user.
     * This method is important for enforcing the business rule "maximum 5 active cards per user".
     *
     * @param userId the ID of the user
     * @return number of active cards
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM payment_cards
        WHERE user_id = :userId AND active = true
        """, nativeQuery = true)
    long countActiveCardsByUserId(@Param("userId") Long userId);
}
