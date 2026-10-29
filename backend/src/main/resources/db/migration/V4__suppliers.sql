CREATE TABLE suppliers (
    id            UUID         PRIMARY KEY,
    business_name VARCHAR(150) NOT NULL,
    cuit          CHAR(11)     NOT NULL UNIQUE,
    vat_condition VARCHAR(30)  NOT NULL,
    address       VARCHAR(150),
    city          VARCHAR(100),
    province      VARCHAR(100),
    phone         VARCHAR(50),
    email         VARCHAR(150),
    active        BOOLEAN      NOT NULL
);

INSERT INTO permissions (code, description) VALUES
    ('SUPPLIERS_VIEW',   'View suppliers'),
    ('SUPPLIERS_MANAGE', 'Create, edit, deactivate and reactivate suppliers');
