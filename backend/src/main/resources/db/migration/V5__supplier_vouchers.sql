CREATE TABLE supplier_vouchers (
    id                       UUID          PRIMARY KEY,
    supplier_id              UUID          NOT NULL REFERENCES suppliers (id),
    type                     VARCHAR(20)   NOT NULL,
    issue_date               DATE          NOT NULL,
    due_date                 DATE,
    point_of_sale            INTEGER       NOT NULL,
    number                   BIGINT        NOT NULL,
    net_taxed_21             NUMERIC(15,2) NOT NULL,
    net_taxed_105            NUMERIC(15,2) NOT NULL,
    net_taxed_27             NUMERIC(15,2) NOT NULL,
    vat_21                   NUMERIC(15,2) NOT NULL,
    vat_105                  NUMERIC(15,2) NOT NULL,
    vat_27                   NUMERIC(15,2) NOT NULL,
    non_taxed                NUMERIC(15,2) NOT NULL,
    exempt                   NUMERIC(15,2) NOT NULL,
    vat_perceptions          NUMERIC(15,2) NOT NULL,
    gross_income_perceptions NUMERIC(15,2) NOT NULL,
    other_taxes              NUMERIC(15,2) NOT NULL,
    CONSTRAINT uq_supplier_voucher UNIQUE (supplier_id, type, point_of_sale, number)
);

CREATE INDEX idx_supplier_vouchers_issue_date ON supplier_vouchers (issue_date);

INSERT INTO permissions (code, description) VALUES
    ('PURCHASES_VIEW',   'View supplier vouchers, the VAT purchases ledger and supplier current accounts'),
    ('PURCHASES_MANAGE', 'Register supplier vouchers');
