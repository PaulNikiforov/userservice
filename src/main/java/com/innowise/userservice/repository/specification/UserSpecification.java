package com.innowise.userservice.repository.specification;

import com.innowise.userservice.model.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Specification<User> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), name.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasSurnameLike(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("surname")), surname.toLowerCase() + "%");
        };
    }

    public static Specification<User> hasActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("active"), active);
        };
    }

    public static Specification<User> filter(String name,
                                             String surname,
                                             Boolean active) {
        return Specification.allOf(
                hasNameLike(name),
                hasSurnameLike(surname),
                hasActive(active)
        );
    }
}
