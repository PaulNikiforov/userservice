package com.innowise.userservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base abstract class for all entities providing JPA auditing capabilities.
 *
 * <p>This class automatically manages timestamp tracking for create and update operations
 * using Spring Data JPA auditing.
 *
 * <p>Auditing features:
 * <ul>
 *   <li>created_at - Automatically set on entity creation, never updated</li>
 *   <li>updated_at - Automatically updated on any entity modification</li>
 *   <li>Automatic timestamp management via Spring Data JPA</li>
 *   <li>Timezone awareness (uses LocalDateTime)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code
 * @Entity
 * public class User extends BaseEntity {
 *     // entity fields inherit createdAt and updatedAt
 * }
 * }
 * </pre>
 *
 * <p>Requirements:
 * <ul>
 *   <li>@EnableJpaAuditing must be configured in Spring application</li>
 *   <li>EntityListeners(AuditingEntityListener.class) must be active</li>
 *   <li>Works with both manual and automatic transaction management</li>
 * </ul>
 *
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 * @see CreatedDate
 * @see LastModifiedDate
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Timestamp when this entity was created.
     * Automatically set by Spring Data JPA on first persist.
     * Never updated afterwards (immutable).
     * Stored in database as 'created_at' column.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this entity was last modified.
     * Automatically updated by Spring Data JPA on each save operation.
     * Stored in database as 'updated_at' column.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
