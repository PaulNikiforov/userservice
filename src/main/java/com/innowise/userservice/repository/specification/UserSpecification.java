package com.innowise.userservice.repository.specification;

import com.innowise.userservice.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Specifications for dynamic filtering of User entities.
 *
 * <p>This class provides type-safe, composable specifications for common user queries.
 * All methods are null-safe and return neutral predicates when parameters are null/blank.
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * Specification<User> spec = UserSpecification.filter("John", null, true, null, null);
 * List<User> users = userRepository.findAll(spec);
 * }
 * </pre>
 */
public final class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Partial case-insensitive match on user's name.
     *
     * @param name Name fragment to search for (null-safe, case-insensitive)
     * @return Specification for name filtering
     */
    public static Specification<User> hasNameLike(String name) {
        return SpecificationUtils.likeIgnoreCase("name", name);
    }

    /**
     * Partial case-insensitive match on user's surname.
     *
     * @param surname Surname fragment to search for (null-safe, case-insensitive)
     * @return Specification for surname filtering
     */
    public static Specification<User> hasSurnameLike(String surname) {
        return SpecificationUtils.likeIgnoreCase("surname", surname);
    }

    /**
     * Partial case-insensitive match on user's email.
     *
     * @param email Email fragment to search for (null-safe, case-insensitive)
     * @return Specification for email filtering
     */
    public static Specification<User> hasEmailLike(String email) {
        return SpecificationUtils.likeIgnoreCase("email", email);
    }

    /**
     * Exact match on user's active status.
     *
     * @param active Active status to filter by (null means ignore this filter)
     * @return Specification for active status filtering
     */
    public static Specification<User> hasActive(Boolean active) {
        return SpecificationUtils.equals("active", active);
    }

    /**
     * Users created on or after the given timestamp.
     *
     * @param from Minimum creation timestamp (null means ignore this filter)
     * @return Specification for creation date filtering
     */
    public static Specification<User> createdAfter(LocalDateTime from) {
        return (root, query, cb) -> {
            if (from == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
        };
    }

    /**
     * Users created on or before the given timestamp.
     *
     * @param to Maximum creation timestamp (null means ignore this filter)
     * @return Specification for creation date filtering
     */
    public static Specification<User> createdBefore(LocalDateTime to) {
        return (root, query, cb) -> {
            if (to == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    /**
     * Combines all user filters into a single composite specification.
     * Null or blank values are ignored (no filtering applied).
     *
     * <p>This is the main entry point for user filtering, combining all available
     * filters with AND logic.
     *
     * @param name Name fragment to search for (null-safe, case-insensitive)
     * @param surname Surname fragment to search for (null-safe, case-insensitive)
     * @param email Email fragment to search for (null-safe, case-insensitive)
     * @param active Active status to filter by (null means ignore)
     * @param createdAtFrom Minimum creation timestamp (null means ignore)
     * @param createdAtTo Maximum creation timestamp (null means ignore)
     * @return Composite specification with all applicable filters
     */
    public static Specification<User> filter(String name,
                                             String surname,
                                             String email,
                                             Boolean active,
                                             LocalDateTime createdAtFrom,
                                             LocalDateTime createdAtTo) {
        return Specification.allOf(
                hasNameLike(name),
                hasSurnameLike(surname),
                hasEmailLike(email),
                hasActive(active),
                createdAfter(createdAtFrom),
                createdBefore(createdAtTo)
        );
    }
}