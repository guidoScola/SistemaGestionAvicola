package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.domain.model.User;
import com.guidoscola.avicola.security.domain.model.Username;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(Username username);

    List<User> findAll();

    boolean existsByRoleId(UUID roleId);
}
