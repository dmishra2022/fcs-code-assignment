package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FulfilmentService Tests")
class FulfilmentServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    private FulfilmentService service;

    @BeforeEach
    void setUp() {
        service = new FulfilmentService();
        try {
            var prodField = FulfilmentService.class.getDeclaredField("productRepository");
            prodField.setAccessible(true);
            prodField.set(service, productRepository);

            var whField = FulfilmentService.class.getDeclaredField("warehouseRepository");
            whField.setAccessible(true);
            whField.set(service, warehouseRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("associateProductWithWarehouse succeeds for new association")
    void associate_validProductAndWarehouse_persists() {
        Product product = new Product("Widget");
        product.id = 1L;
        product.fulfilmentUnits = new ArrayList<>();

        DbWarehouse warehouse = new DbWarehouse();
        warehouse.id = 10L;

        when(productRepository.findById(1L)).thenReturn(product);
        when(warehouseRepository.findById(10L)).thenReturn(warehouse);

        assertDoesNotThrow(() -> service.associateProductWithWarehouse(1L, 10L));

        assertTrue(product.fulfilmentUnits.contains(warehouse));
        verify(productRepository).persist(product);
    }

    @Test
    @DisplayName("associateProductWithWarehouse skips if already associated")
    void associate_alreadyAssociated_doesNotDuplicate() {
        DbWarehouse warehouse = new DbWarehouse();
        warehouse.id = 10L;

        Product product = new Product("Widget");
        product.id = 1L;
        product.fulfilmentUnits = new ArrayList<>();
        product.fulfilmentUnits.add(warehouse);

        when(productRepository.findById(1L)).thenReturn(product);
        when(warehouseRepository.findById(10L)).thenReturn(warehouse);

        service.associateProductWithWarehouse(1L, 10L);

        assertEquals(1, product.fulfilmentUnits.size());
        verify(productRepository, never()).persist(any(Product.class));
    }

    @Test
    @DisplayName("associateProductWithWarehouse throws when product not found")
    void associate_productNotFound_throwsValidationException() {
        when(productRepository.findById(99L)).thenReturn(null);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> service.associateProductWithWarehouse(99L, 10L));
        assertTrue(ex.getMessage().contains("Product not found"));
    }

    @Test
    @DisplayName("associateProductWithWarehouse throws when warehouse not found")
    void associate_warehouseNotFound_throwsValidationException() {
        Product product = new Product("Widget");
        product.id = 1L;
        when(productRepository.findById(1L)).thenReturn(product);
        when(warehouseRepository.findById(99L)).thenReturn(null);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> service.associateProductWithWarehouse(1L, 99L));
        assertTrue(ex.getMessage().contains("Warehouse not found"));
    }

    @Test
    @DisplayName("associateProductWithWarehouse throws when warehouse is archived")
    void associate_archivedWarehouse_throwsValidationException() {
        Product product = new Product("Widget");
        product.id = 1L;

        DbWarehouse warehouse = new DbWarehouse();
        warehouse.id = 10L;
        warehouse.archivedAt = LocalDateTime.now();

        when(productRepository.findById(1L)).thenReturn(product);
        when(warehouseRepository.findById(10L)).thenReturn(warehouse);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> service.associateProductWithWarehouse(1L, 10L));
        assertTrue(ex.getMessage().contains("archived"));
    }
}
