package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class FulfilmentService {

    @Inject
    ProductRepository productRepository;

    @Inject
    WarehouseRepository warehouseRepository;

    @Inject
    EntityManager entityManager;

    private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;
    private static final int MAX_WAREHOUSES_PER_STORE = 3;
    private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;

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

        if (product.fulfilmentUnits.contains(dbWarehouse)) {
            return;
        }

        long productCount = productRepository.countByWarehouseId(warehouseId);
        if (productCount >= MAX_PRODUCTS_PER_WAREHOUSE) {
            throw new WarehouseValidationException(
                    "Warehouse can store a maximum of " + MAX_PRODUCTS_PER_WAREHOUSE + " types of products.");
        }

        checkMaxWarehousesPerProductPerStore(product, dbWarehouse);

        product.fulfilmentUnits.add(dbWarehouse);
        productRepository.persist(product);
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

        if (store.fulfilmentUnits.contains(dbWarehouse)) {
            return;
        }

        if (store.fulfilmentUnits.size() >= MAX_WAREHOUSES_PER_STORE) {
            throw new WarehouseValidationException(
                    "Store can be fulfilled by a maximum of " + MAX_WAREHOUSES_PER_STORE + " warehouses.");
        }

        store.fulfilmentUnits.add(dbWarehouse);
        store.persist();
    }

    private void checkMaxWarehousesPerProductPerStore(Product product, DbWarehouse newWarehouse) {
        Set<Long> productWarehouseIds = product.fulfilmentUnits.stream()
                .map(w -> w.id)
                .collect(Collectors.toSet());
        productWarehouseIds.add(newWarehouse.id);

        List<Store> storesWithWarehouse = entityManager.createQuery(
                        "SELECT DISTINCT s FROM Store s JOIN s.fulfilmentUnits w WHERE w.id IN :wids", Store.class)
                .setParameter("wids", productWarehouseIds)
                .getResultList();

        for (Store store : storesWithWarehouse) {
            long shared = store.fulfilmentUnits.stream()
                    .filter(w -> productWarehouseIds.contains(w.id))
                    .count();
            if (shared > MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
                throw new WarehouseValidationException(
                        "Product can be fulfilled by a maximum of " + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
                                + " warehouses per store.");
            }
        }
    }
}
