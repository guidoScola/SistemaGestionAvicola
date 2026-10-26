package com.guidoscola.avicola.security.infrastructure.persistence;

import com.guidoscola.avicola.security.application.port.UserRepository;
import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return jpa.save(UserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll(Sort.by("username")).stream().map(UserEntity::toDomain).toList();
    }

    @Override
    public boolean existsByRoleId(UUID roleId) {
        return jpa.existsByRoleId(roleId);
    }
}
