package com.guidoscola.avicola.security.application.port;

import com.guidoscola.avicola.security.application.AccessToken;
import com.guidoscola.avicola.security.domain.model.PermissionCode;
import com.guidoscola.avicola.security.domain.model.User;

import java.util.Set;

@FunctionalInterface
public interface AccessTokenIssuer {

    AccessToken issue(User user, Set<PermissionCode> permissions);
}
