package com.innowise.userservice.repository;

import com.innowise.userservice.model.User;
import com.innowise.userservice.repository.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists by email.
     *
     * @param email the email to check
     * @return true if a user with the email exists
     */
    boolean existsByEmail(String email);


    /**
     * Finds all users filtered by active status with pagination.
     *
     * @param active   the active status to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of users matching the active status
     */
    @Query("SELECT u FROM User u WHERE u.active = :active")
    Page<User> findByActive(@Param("active") Boolean active, Pageable pageable);


    /**
     * Finds users by partial name and surname match using Specification composition.
     *
     * @param name     partial match for name
     * @param surname  partial match for surname
     * @param pageable pagination and sorting information
     * @return a page of matching users
     */
    default Page<User> findAllByNameAndSurname(String name, String surname, Pageable pageable) {
        var spec = UserSpecification.hasNameLike(name)
                .and(UserSpecification.hasSurnameLike(surname));
        return findAll(spec, pageable);
    }

    /**
     * Finds a user by ID with pessimistic write lock.
     * Use this when you need to prevent concurrent modifications (e.g., checking limits before creating entities).
     *
     * @param id the user ID to search for
     * @return an Optional containing the user if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByIdWithLock(Long id);
}