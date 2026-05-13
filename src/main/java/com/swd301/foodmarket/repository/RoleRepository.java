package com.swd301.foodmarket.repository;

import com.swd301.foodmarket.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.swd301.foodmarket.entity.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);

}
