package com.guidoscola.avicola.purchases.infrastructure;

import com.guidoscola.avicola.purchases.application.PurchaseReportsService;
import com.guidoscola.avicola.purchases.application.SupplierManagementService;
import com.guidoscola.avicola.purchases.application.SupplierVoucherService;
import com.guidoscola.avicola.purchases.application.port.SupplierRepository;
import com.guidoscola.avicola.purchases.application.port.SupplierVoucherRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PurchasesUseCasesConfig {

    @Bean
    SupplierManagementService supplierManagementService(SupplierRepository suppliers) {
        return new SupplierManagementService(suppliers);
    }

    @Bean
    SupplierVoucherService supplierVoucherService(SupplierVoucherRepository vouchers, SupplierRepository suppliers) {
        return new SupplierVoucherService(vouchers, suppliers);
    }

    @Bean
    PurchaseReportsService purchaseReportsService(SupplierVoucherRepository vouchers, SupplierRepository suppliers) {
        return new PurchaseReportsService(vouchers, suppliers);
    }
}
