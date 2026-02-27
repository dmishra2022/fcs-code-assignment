package com.fulfilment.application.monolith.fulfilment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    @Mock
    private EntityManager entityManager;
    @SuppressWarnings("unchecked")
    @Mock
    private TypedQuery<Object> typedQuery;

    private FulfilmentService service;

    @BeforeEach
    void setUp() {
        service = new FulfilmentService();
        try {
            for (var entry : new Object[][] {
                    { "productRepository", productRepository },
                    { "warehouseRepository", warehouseRepository },
                    { "entityManager", entityManager }
            }) {
                var field = FulfilmentService.class.getDeclaredField((String) entry[0]);
                field.setAccessible(true);
                field.set(service, entry[1]);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void stubStoreQuery() {
        lenient().when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        lenient().when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        lenient().when(typedQuery.getResultList()).thenReturn(Collections.emptyList());
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
        when(productRepository.countByWarehouseId(10L)).thenReturn(0L);
        stubStoreQuery();

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

    @Test
    @DisplayName("associateProductWithWarehouse throws when warehouse has max products")
    void associate_maxProductsPerWarehouse_throwsValidationException() {
        Product product = new Product("Widget");
        product.id = 1L;
        product.fulfilmentUnits = new ArrayList<>();

        DbWarehouse warehouse = new DbWarehouse();
        warehouse.id = 10L;

        when(productRepository.findById(1L)).thenReturn(product);
        when(warehouseRepository.findById(10L)).thenReturn(warehouse);
        when(productRepository.countByWarehouseId(10L)).thenReturn(5L);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> service.associateProductWithWarehouse(1L, 10L));
        assertTrue(ex.getMessage().contains("maximum of 5"));
    }
}
