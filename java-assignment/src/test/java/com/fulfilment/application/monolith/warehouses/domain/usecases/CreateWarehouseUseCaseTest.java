package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateWarehouseUseCase Tests")
class CreateWarehouseUseCaseTest {

    @Mock
    private WarehouseStore warehouseStore;
    @Mock
    private LocationResolver locationResolver;

    private CreateWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
    }

    // ─── Happy path ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() succeeds with valid warehouse data")
    void create_validWarehouse_persistsWarehouse() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "AMSTERDAM-001", 80, 20);
        Location location = new Location("AMSTERDAM-001", 5, 100);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        lenient().when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> useCase.create(warehouse));
        verify(warehouseStore).create(warehouse);
    }

    // ─── Business Unit Code uniqueness ───────────────────────────────────────

    @Test
    @DisplayName("create() throws when business unit code already exists")
    void create_duplicateBuCode_throwsValidationException() {
        Warehouse existing = warehouseWithCode("MWH.001", "AMSTERDAM-001", 50, 10);
        Warehouse newWarehouse = warehouseWithCode("MWH.001", "TILBURG-001", 30, 5);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> useCase.create(newWarehouse));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(warehouseStore, never()).create(any());
    }

    // ─── Location validation ──────────────────────────────────────────────────

    @Test
    @DisplayName("create() throws when location is unknown")
    void create_unknownLocation_throwsValidationException() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "NONEXISTENT-001", 30, 5);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("NONEXISTENT-001")).thenReturn(null);

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> useCase.create(warehouse));
        assertTrue(ex.getMessage().contains("does not exist"));
        verify(warehouseStore, never()).create(any());
    }

    // ─── Max warehouse count ──────────────────────────────────────────────────

    @Test
    @DisplayName("create() throws when location has reached max warehouse count")
    void create_locationAtMaxCapacity_throwsValidationException() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "ZWOLLE-001", 30, 5);
        Location location = new Location("ZWOLLE-001", 1, 40);
        Warehouse existingAtLocation = warehouseWithCode("MWH.001", "ZWOLLE-001", 30, 10);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        lenient().when(warehouseStore.findActiveByLocation("ZWOLLE-001")).thenReturn(List.of(existingAtLocation));

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> useCase.create(warehouse));
        assertTrue(ex.getMessage().contains("maximum number of warehouses"));
        verify(warehouseStore, never()).create(any());
    }

    // ─── Capacity validation ──────────────────────────────────────────────────

    @Test
    @DisplayName("create() throws when requested capacity exceeds location max")
    void create_capacityExceedsLocationMax_throwsValidationException() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "ZWOLLE-001", 50, 5); // max is 40
        Location location = new Location("ZWOLLE-001", 1, 40);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        lenient().when(warehouseStore.findActiveByLocation("ZWOLLE-001")).thenReturn(Collections.emptyList());

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> useCase.create(warehouse));
        assertTrue(ex.getMessage().contains("exceeds the maximum allowed capacity"));
        verify(warehouseStore, never()).create(any());
    }

    @Test
    @DisplayName("create() succeeds when capacity equals location max (boundary)")
    void create_capacityEqualsLocationMax_succeeds() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "ZWOLLE-001", 40, 5);
        Location location = new Location("ZWOLLE-001", 1, 40);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);
        lenient().when(warehouseStore.findActiveByLocation("ZWOLLE-001")).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> useCase.create(warehouse));
        verify(warehouseStore).create(warehouse);
    }

    // ─── Stock validation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("create() throws when stock exceeds capacity")
    void create_stockExceedsCapacity_throwsValidationException() {
        Warehouse warehouse = warehouseWithCode("MWH.NEW", "AMSTERDAM-001", 30, 40); // stock > capacity
        Location location = new Location("AMSTERDAM-001", 5, 100);

        lenient().when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
        lenient().when(locationResolver.resolveByIdentifier("AMSTERDAM-001")).thenReturn(location);
        lenient().when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

        WarehouseValidationException ex = assertThrows(WarehouseValidationException.class,
                () -> useCase.create(warehouse));
        assertTrue(ex.getMessage().contains("Stock"));
        verify(warehouseStore, never()).create(any());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Warehouse warehouseWithCode(String buCode, String location, int capacity, int stock) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = location;
        w.capacity = capacity;
        w.stock = stock;
        return w;
    }
}
