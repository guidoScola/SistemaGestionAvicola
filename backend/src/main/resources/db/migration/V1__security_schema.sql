CREATE TABLE permissions (
    code        VARCHAR(64)  PRIMARY KEY,
    description VARCHAR(200) NOT NULL
);

CREATE TABLE roles (
    id   UUID        PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX ux_roles_name_lower ON roles (LOWER(name));

CREATE TABLE role_permissions (
    role_id         UUID        NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_code VARCHAR(64) NOT NULL REFERENCES permissions (code),
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active        BOOLEAN      NOT NULL
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO permissions (code, description) VALUES
    ('USERS_VIEW',   'View users'),
    ('USERS_MANAGE', 'Create, edit, activate and deactivate users'),
    ('ROLES_VIEW',   'View roles and permissions'),
    ('ROLES_MANAGE', 'Create, edit and delete roles');
