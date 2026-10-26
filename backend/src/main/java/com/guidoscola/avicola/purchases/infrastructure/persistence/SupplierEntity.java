package com.guidoscola.avicola.purchases.infrastructure.persistence;

import com.guidoscola.avicola.purchases.domain.model.Cuit;
import com.guidoscola.avicola.purchases.domain.model.Supplier;
import com.guidoscola.avicola.purchases.domain.model.VatCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
class SupplierEntity {

    @Id
    private UUID id;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, unique = true, length = 11)
    @JdbcTypeCode(Types.CHAR)
    private String cuit;

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_condition", nullable = false, length = 30)
    private VatCondition vatCondition;

    @Column(length = 150)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(nullable = false)
    private boolean active;

    protected SupplierEntity() {
    }

    static SupplierEntity fromDomain(Supplier supplier) {
        SupplierEntity entity = new SupplierEntity();
        entity.id = supplier.id();
        entity.businessName = supplier.businessName();
        entity.cuit = supplier.cuit().value();
        entity.vatCondition = supplier.vatCondition();
        entity.address = supplier.address();
        entity.city = supplier.city();
        entity.province = supplier.province();
        entity.phone = supplier.phone();
        entity.email = supplier.email();
        entity.active = supplier.isActive();
        return entity;
    }

    Supplier toDomain() {
        return new Supplier(id, businessName, new Cuit(cuit), vatCondition, address, city, province, phone, email,
                active);
    }
}
