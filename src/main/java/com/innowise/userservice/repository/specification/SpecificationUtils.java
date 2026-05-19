package com.innowise.userservice.repository.specification;

import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class providing common specification patterns to eliminate code duplication.
 *
 * <p>This class contains reusable specification builders that implement common
 * query patterns used throughout the application.
 *
 * <p>Features:
 * <ul>
 *   <li>Case-insensitive partial matching (LIKE queries)</li>
 *   <li>null-safe operations</li>
 *   <li>Reusable across different entity types</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>
 * {@code
 * public static Specification<User> hasNameLike(String name) {
 *     return SpecificationUtils.likeIgnoreCase("name", name);
 * }
 * }
 * </pre>
 */
public final class SpecificationUtils {

    private SpecificationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Creates a case-insensitive partial matching specification for string fields.
     *
     * <p>This specification performs a case-insensitive LIKE query with wildcards,
     * allowing partial matches. Null or blank values result in a neutral predicate
     * that doesn't filter any results.
     *
     * <p>Examples:
     * <ul>
     *   <li>"John" matches "john", "JOHN", "Johnson"</li>
     *   <li>null or "" matches all values (no filtering)</li>
     * </ul>
     *
     * @param fieldName The name of the entity field to query (required)
     * @param value The value to search for (null-safe, blank values ignored)
     * @param <T> The entity type
     * @return A specification that performs case-insensitive partial matching
     * @throws IllegalArgumentException if fieldName is null or blank
     */
    public static <T> Specification<T> likeIgnoreCase(String fieldName, String value) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }

        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }

            // Convert both field value and search term to lowercase for case-insensitive matching
            // Using LOWER() function instead of lower() for compatibility
            return cb.like(
                cb.lower(root.get(fieldName)),
                "%" + value.toLowerCase() + "%"
            );
        };
    }

    /**
     * Creates an equality specification for comparable fields.
     *
     * <p>This specification performs an exact match query. Null values result in
     * a neutral predicate that doesn't filter any results.
     *
     * @param fieldName The name of the entity field to query (required)
     * @param value The value to match exactly (null-safe)
     * @param <T> The entity type
     * @return A specification that performs exact matching
     * @throws IllegalArgumentException if fieldName is null or blank
     */
    public static <T> Specification<T> equals(String fieldName, Object value) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }

        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction(); // Neutral predicate - don't filter
            }

            return cb.equal(root.get(fieldName), value);
        };
    }

    /**
     * Creates a boolean equality specification for boolean fields.
     *
     * <p>This specification performs an exact match for boolean/Boolean fields.
     * Null values result in a neutral predicate that doesn't filter any results.
     *
     * @param fieldName The name of the boolean entity field to query (required)
     * @param value The boolean value to match (null-safe)
     * @param <T> The entity type
     * @return A specification that performs boolean equality matching
     * @throws IllegalArgumentException if fieldName is null or blank
     */
    public static <T> Specification<T> equals(String fieldName, Boolean value) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }

        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get(fieldName), value);
        };
    }
}
