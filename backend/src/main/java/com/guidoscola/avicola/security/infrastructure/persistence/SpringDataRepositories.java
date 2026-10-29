package com.guidoscola.avicola.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    @Query("select count(u) > 0 from UserEntity u join u.roleIds r where r = :roleId")
    boolean existsByRoleId(@Param("roleId") UUID roleId);
}

interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByNameIgnoreCase(String name);
}

interface PermissionJpaRepository extends JpaRepository<PermissionEntity, String> {
}

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("update RefreshTokenEntity t set t.revoked = true where t.id = :id and t.revoked = false")
    int revokeIfActive(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("update RefreshTokenEntity t set t.revoked = true where t.userId = :userId and t.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);
}
