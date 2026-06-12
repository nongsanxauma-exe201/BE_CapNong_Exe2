package com.swd301.foodmarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.swd301.foodmarket.entity.User;

import java.time.LocalDateTime;

import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u WHERE u.status = 'INACTIVE' AND u.lockedAt <= :time")
    List<User> findUsersLockedBefore(LocalDateTime time);

    Optional<User> findByEmail(String email);
    List<User> findByRole_NameIn(List<RoleName> roleNames);
    boolean existsByRole_Name(RoleName name);

    Optional<User> findFirstByRole_Name(RoleName roleName);

    Page<User> findAll(Pageable pageable);
    Page<User> findByRole_NameIn(List<RoleName> roleNames, Pageable pageable);
    Page<User> findByRole_NameInAndStatus(List<RoleName> roleNames, UserStatus status, Pageable pageable);

    Long countByCreateAtBetween(LocalDateTime createAtAfter, LocalDateTime createAtBefore);

    Long countByRole_NameAndCreateAtBetween(RoleName roleName, LocalDateTime createAtAfter, LocalDateTime createAtBefore);

    Long countByRole_NameAndStatusAndCreateAtBetween(RoleName roleName, UserStatus status, LocalDateTime createAtAfter, LocalDateTime createAtBefore);

    Long countByRole_Name(RoleName roleName);

    Long countByStatus(UserStatus status);

    Long countByRole_NameAndShopActive(RoleName roleName, boolean shopActive);

    @Query("""
        SELECT COUNT(u) FROM User u
        WHERE u.lockedAt IS NOT NULL 
""")
    Long countUserLocked();

    Long countByLockedAtBetween(LocalDateTime lockedAtAfter, LocalDateTime lockedAtBefore);

    Long countByRole_NameAndStatus(RoleName roleName, UserStatus status);

    Long countByRole_NameAndCreateAtGreaterThanEqualAndCreateAtLessThan(RoleName roleName, LocalDateTime createAtIsGreaterThan, LocalDateTime createAtIsLessThan);

    Long countByLockedAtGreaterThanEqualAndLockedAtLessThan(LocalDateTime lockedAtIsGreaterThan, LocalDateTime lockedAtIsLessThan);
}
