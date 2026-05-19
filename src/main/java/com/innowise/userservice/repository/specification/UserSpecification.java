package com.innowise.userservice.repository.specification;

import com.innowise.userservice.model.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications for dynamic filtering of User entities.
 *
 * <p>This class provides type-safe, composable specifications for common user queries.
 * All methods are null-safe and return neutral predicates when parameters are null/blank.
 */
public final class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Partial case-insensitive match on user's name.
     */
    public static Specification<User> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Partial case-insensitive match on user's surname.
     */
    public static Specification<User> hasSurnameLike(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
        };
    }

    /**
     * Partial case-insensitive match on user's email.
     */
    public static Specification<User> hasEmailLike(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    /**
     * Exact match on user's active status.
     */
    public static Specification<User> hasActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    /**
     * Combines all user filters into a single composite specification.
     */
    public static Specification<User> filter(String name,
                                             String surname,
                                             String email,
                                             Boolean active) {
        return Specification.allOf(
                hasNameLike(name),
                hasSurnameLike(surname),
                hasEmailLike(email),
                hasActive(active)
        );
    }
}