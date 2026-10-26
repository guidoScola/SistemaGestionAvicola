package com.guidoscola.avicola.purchases.infrastructure;

import com.guidoscola.avicola.purchases.application.SupplierManagementService;
import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PurchasesUseCasesConfig {

    @Bean
    SupplierManagementService supplierManagementService(SupplierRepository suppliers) {
        return new SupplierManagementService(suppliers);
    }
}
