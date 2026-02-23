package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReplaceWarehouseUseCase Tests")
class ReplaceWarehouseUseCaseTest {

    @Mock private WarehouseStore warehouseStore;
    @Mock private LocationResolver locationResolver;

    private ReplaceWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
    }

    // ─── Happy path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("replace() archives existing and creates new warehouse")
    void replace_validReplacement_archivesOldAndCreatesNew() {
        Warehouse existing = warehouseWith("MWH.001", "ZWOLLE-001", 30, 10);
        Warehouse newWarehouse = warehouseWith("MWH.001", "AMSTERDAM-001", 50, 10); // same stock
        Location location = new Location("AMSTERDAM-001", 5, 100);

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);

        assertDoesNotThrow(() -> useCase.replace(newWarehouse));

        // Old warehouse should be archived
        ArgumentCaptor<Warehouse> updateCaptor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseStore).update(updateCaptor.capture());
        assertNotNull(updateCaptor.getValue().archivedAt);

        // New warehouse should be created
        verify(warehouseStore).create(newWarehouse);
    }

    // ─── Non-existent warehouse ───────────────────────────────────────────────

    @Test
    @DisplayName("replace() throws when warehouse does not exist")
    void replace_warehouseNotFound_throwsValidationException() {
        Warehouse newWarehouse = warehouseWith("MWH.UNKNOWN", "AMSTERDAM-001", 50, 10);

        when(warehouseStore.findByBusinessUnitCode("MWH.UNKNOWN")).thenReturn(null);

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
        assertTrue(ex.getMessage().contains("No active warehouse found"));
        verify(warehouseStore, never()).update(any());
        verify(warehouseStore, never()).create(any());
    }

    // ─── Invalid location ─────────────────────────────────────────────────────

    @Test
    @DisplayName("replace() throws when new location is invalid")
    void replace_invalidLocation_throwsValidationException() {
        Warehouse existing = warehouseWith("MWH.001", "ZWOLLE-001", 30, 10);
        Warehouse newWarehouse = warehouseWith("MWH.001", "INVALID-999", 30, 10);

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("INVALID-999")).thenReturn(null);

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    // ─── Capacity must accommodate existing stock ─────────────────────────────

    @Test
    @DisplayName("replace() throws when new capacity cannot accommodate old stock")
    void replace_newCapacityTooSmall_throwsValidationException() {
        Warehouse existing = warehouseWith("MWH.001", "ZWOLLE-001", 50, 30); // stock=30
        Warehouse newWarehouse = warehouseWith("MWH.001", "AMSTERDAM-001", 20, 30); // cap=20 < stock
        Location location = new Location("AMSTERDAM-001", 5, 100);

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
        assertTrue(ex.getMessage().contains("cannot accommodate the existing stock"));
    }

    // ─── Stock must match ─────────────────────────────────────────────────────

    @Test
    @DisplayName("replace() throws when new stock does not match old stock")
    void replace_stockMismatch_throwsValidationException() {
        Warehouse existing = warehouseWith("MWH.001", "ZWOLLE-001", 50, 30);
        Warehouse newWarehouse = warehouseWith("MWH.001", "AMSTERDAM-001", 60, 15); // stock mismatch
        Location location = new Location("AMSTERDAM-001", 5, 100);

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
        assertTrue(ex.getMessage().contains("must match the current stock"));
    }

    // ─── Capacity exceeds location max ───────────────────────────────────────

    @Test
    @DisplayName("replace() throws when new capacity exceeds location max")
    void replace_capacityExceedsLocationMax_throwsValidationException() {
        Warehouse existing = warehouseWith("MWH.001", "ZWOLLE-001", 30, 10);
        Warehouse newWarehouse = warehouseWith("MWH.001", "ZWOLLE-001", 50, 10); // max is 40
        Location location = new Location("ZWOLLE-001", 1, 40);

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
        when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed capacity"));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Warehouse warehouseWith(String buCode, String location, int capacity, int stock) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = location;
        w.capacity = capacity;
        w.stock = stock;
        return w;
    }
}
