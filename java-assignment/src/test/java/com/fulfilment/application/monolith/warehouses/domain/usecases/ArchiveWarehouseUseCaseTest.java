package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveWarehouseUseCase Tests")
class ArchiveWarehouseUseCaseTest {

    @Mock private WarehouseStore warehouseStore;

    private ArchiveWarehouseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ArchiveWarehouseUseCase(warehouseStore);
    }

    @Test
    @DisplayName("archive() sets archivedAt timestamp on existing warehouse")
    void archive_existingWarehouse_setsArchivedAt() {
        Warehouse existing = warehouseWith("MWH.001");
        Warehouse request = warehouseWith("MWH.001");

        when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);

        assertDoesNotThrow(() -> useCase.archive(request));

        ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
        verify(warehouseStore).update(captor.capture());
        assertNotNull(captor.getValue().archivedAt, "archivedAt must be set after archiving");
    }

    @Test
    @DisplayName("archive() throws when warehouse does not exist")
    void archive_nonExistentWarehouse_throwsValidationException() {
        when(warehouseStore.findByBusinessUnitCode("MWH.MISSING")).thenReturn(null);

        Warehouse request = warehouseWith("MWH.MISSING");

        WarehouseValidationException ex =
                assertThrows(WarehouseValidationException.class, () -> useCase.archive(request));
        assertTrue(ex.getMessage().contains("No active warehouse found"));
        verify(warehouseStore, never()).update(any());
    }

    @Test
    @DisplayName("archive() does not call update() when warehouse not found")
    void archive_notFound_neverCallsUpdate() {
        when(warehouseStore.findByBusinessUnitCode(anyString())).thenReturn(null);
        Warehouse request = warehouseWith("MWH.GHOST");

        assertThrows(WarehouseValidationException.class, () -> useCase.archive(request));
        verify(warehouseStore, never()).update(any());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Warehouse warehouseWith(String buCode) {
        Warehouse w = new Warehouse();
        w.businessUnitCode = buCode;
        w.location = "AMSTERDAM-001";
        w.capacity = 50;
        w.stock = 10;
        return w;
    }
}
