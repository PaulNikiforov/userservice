package com.innowise.userservice.repository;

import com.innowise.userservice.model.PaymentCard;
import com.innowise.userservice.repository.specification.PaymentCardSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentCardRepository extends
        JpaRepository<PaymentCard, Long>,
        JpaSpecificationExecutor<PaymentCard> {

    /**
     * Finds all payment cards belonging to a specific user with pagination.
     *
     * @param userId   the ID of the user
     * @param pageable pagination and sorting parameters
     * @return a page of payment cards
     */
    Page<PaymentCard> findByUserId(Long userId, Pageable pageable);


    /**
     * Finds all payment cards of a user filtered by active status
     *
     * @param userId the ID of the user
     * @param active filter by active status
     * @return list of matching payment cards
     */
    List<PaymentCard> findByUserIdAndActive(Long userId, Boolean active);


    /**
     * Finds cards by user ID
     *
     * @param userId   the ID of the user
     * @param pageable pagination and sorting parameters
     * @return a page of payment cards
     */
    @Query("SELECT pc FROM PaymentCard pc WHERE pc.user.id = :userId")
    Page<PaymentCard> findByUserIdJpql(@Param("userId") Long userId, Pageable pageable);


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


    /**
     * Finds payment cards using dynamic filters with pagination.
     * Uses Specification for flexible querying.
     *
     * @param userId          filter by user ID
     * @param holder          partial match on holder name
     * @param active          filter by active status
     * @param expirationFrom  expiration date lower bound
     * @param expirationTo    expiration date upper bound
     * @param pageable        pagination and sorting
     * @return a page of matching payment cards
     */
    default Page<PaymentCard> findAllByFilter(Long userId,
                                              String holder,
                                              Boolean active,
                                              LocalDate expirationFrom,
                                              LocalDate expirationTo,
                                              Pageable pageable) {

        Specification<PaymentCard> spec = PaymentCardSpecification.filter(
                userId, holder, active, expirationFrom, expirationTo
        );

        return findAll(spec, pageable);
    }
}