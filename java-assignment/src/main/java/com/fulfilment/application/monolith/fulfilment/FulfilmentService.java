package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class FulfilmentService {

    @Inject
    ProductRepository productRepository;

    @Inject
    WarehouseRepository warehouseRepository;

    @Transactional
    public void associateProductWithWarehouse(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId);
        if (product == null) {
            throw new WarehouseValidationException("Product not found with id: " + productId);
        }

        DbWarehouse dbWarehouse = warehouseRepository.findById(warehouseId);
        if (dbWarehouse == null) {
            throw new WarehouseValidationException("Warehouse not found with id: " + warehouseId);
        }

        if (dbWarehouse.archivedAt != null) {
            throw new WarehouseValidationException("Cannot associate an archived warehouse.");
        }

        if (!product.fulfilmentUnits.contains(dbWarehouse)) {
            product.fulfilmentUnits.add(dbWarehouse);
            productRepository.persist(product);
        }
    }

    @Transactional
    public void associateStoreWithWarehouse(Long storeId, Long warehouseId) {
        Store store = Store.findById(storeId);
        if (store == null) {
            throw new WarehouseValidationException("Store not found with id: " + storeId);
        }

        DbWarehouse dbWarehouse = warehouseRepository.findById(warehouseId);
        if (dbWarehouse == null) {
            throw new WarehouseValidationException("Warehouse not found with id: " + warehouseId);
        }

        if (dbWarehouse.archivedAt != null) {
            throw new WarehouseValidationException("Cannot associate an archived warehouse.");
        }

        if (!store.fulfilmentUnits.contains(dbWarehouse)) {
            store.fulfilmentUnits.add(dbWarehouse);
            store.persist();
        }
    }
}
