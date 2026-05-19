package com.innowise.userservice.repository.specification;

import com.innowise.userservice.model.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Specifications for dynamic filtering of PaymentCard entities.
 *
 * <p>This class provides type-safe, composable specifications for common payment card queries.
 * All methods are null-safe and return neutral predicates when parameters are null/blank.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * Specification<PaymentCard> spec = PaymentCardSpecification.filter(userId, "John", true, null, null);
 * List<PaymentCard> cards = paymentCardRepository.findAll(spec);
 * }
 * </pre>
 */
public final class PaymentCardSpecification {

    private PaymentCardSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Partial case-insensitive match on cardholder name.
     *
     * @param holder Cardholder name fragment to search for (null-safe, case-insensitive)
     * @return Specification for holder filtering
     */
    public static Specification<PaymentCard> hasHolderLike(String holder) {
        return SpecificationUtils.likeIgnoreCase("holder", holder);
    }

    /**
     * Exact match on card's active status.
     *
     * @param active Active status to filter by (null means ignore this filter)
     * @return Specification for active status filtering
     */
    public static Specification<PaymentCard> hasActive(Boolean active) {
        return SpecificationUtils.equals("active", active);
    }

    /**
     * Cards with expiration date on or after the given date.
     *
     * @param from Minimum expiration date (null means ignore this filter)
     * @return Specification for expiration date filtering
     */
    public static Specification<PaymentCard> expirationAfter(LocalDate from) {
        return (root, query, cb) -> {
            if (from == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("expirationDate"), from);
        };
    }

    /**
     * Cards with expiration date on or before the given date.
     *
     * @param to Maximum expiration date (null means ignore this filter)
     * @return Specification for expiration date filtering
     */
    public static Specification<PaymentCard> expirationBefore(LocalDate to) {
        return (root, query, cb) -> {
            if (to == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("expirationDate"), to);
        };
    }

    /**
     * Cards belonging to a specific user.
     *
     * @param userId User ID to filter by (null means ignore this filter)
     * @return Specification for user filtering
     */
    public static Specification<PaymentCard> belongsToUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    /**
     * Combines all payment card filters into a single composite specification.
     * Null or blank values are ignored (no filtering applied).
     *
     * <p>This is the main entry point for payment card filtering, combining all available
     * filters with AND logic.
     *
     * @param userId User ID to filter by (null means ignore)
     * @param holder Cardholder name fragment (null-safe, case-insensitive)
     * @param active Active status to filter by (null means ignore)
     * @param expirationFrom Minimum expiration date (null means ignore)
     * @param expirationTo Maximum expiration date (null means ignore)
     * @return Composite specification with all applicable filters
     */
    public static Specification<PaymentCard> filter(Long userId,
                                                    String holder,
                                                    Boolean active,
                                                    LocalDate expirationFrom,
                                                    LocalDate expirationTo) {

        return Specification.allOf(
                belongsToUser(userId),
                hasHolderLike(holder),
                hasActive(active),
                expirationAfter(expirationFrom),
                expirationBefore(expirationTo)
        );
    }
}
