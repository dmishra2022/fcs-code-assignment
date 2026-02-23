package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseRepositoryTest {

    @Inject
    WarehouseRepository repository;

    @Test
    @TestTransaction
    public void testGetAllActive() {
        List<Warehouse> active = repository.getAll();
        assertFalse(active.isEmpty());
        assertTrue(active.stream().allMatch(w -> w.businessUnitCode.startsWith("MWH.")));
    }

    @Test
    @TestTransaction
    public void testFindByBusinessUnitCode() {
        Warehouse w = repository.findByBusinessUnitCode("MWH.001");
        assertNotNull(w);
        assertEquals("MWH.001", w.businessUnitCode);
    }

    @Test
    @TestTransaction
    public void testFindByBusinessUnitCodeNotFound() {
        Warehouse w = repository.findByBusinessUnitCode("NONEXISTENT");
        assertNull(w);
    }

    @Test
    @TestTransaction
    public void testCreateWarehouse() {
        Warehouse w = new Warehouse();
        w.businessUnitCode = "MWH.TEST";
        w.location = "AMSTERDAM-001";
        w.capacity = 100;
        w.stock = 10;

        repository.create(w);

        Warehouse persisted = repository.findByBusinessUnitCode("MWH.TEST");
        assertNotNull(persisted);
        assertEquals("MWH.TEST", persisted.businessUnitCode);
    }

    @Test
    @TestTransaction
    public void testUpdateWarehouse() {
        Warehouse w = repository.findByBusinessUnitCode("MWH.012");
        w.capacity = 999;
        repository.update(w);

        Warehouse updated = repository.findByBusinessUnitCode("MWH.012");
        assertEquals(999, updated.capacity);
    }

    @Test
    @TestTransaction
    public void testRemoveWarehouse() {
        Warehouse w = repository.findByBusinessUnitCode("MWH.023");
        assertNotNull(w);
        repository.remove(w);

        Warehouse removed = repository.findByBusinessUnitCode("MWH.023");
        assertNull(removed);
    }
}
