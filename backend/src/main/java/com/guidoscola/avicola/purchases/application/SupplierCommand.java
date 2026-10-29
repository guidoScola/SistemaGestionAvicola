package com.guidoscola.avicola.purchases.application;

import com.guidoscola.avicola.purchases.domain.model.VatCondition;

public record SupplierCommand(String businessName, String cuit, VatCondition vatCondition, String address,
                              String city, String province, String phone, String email) {
}
