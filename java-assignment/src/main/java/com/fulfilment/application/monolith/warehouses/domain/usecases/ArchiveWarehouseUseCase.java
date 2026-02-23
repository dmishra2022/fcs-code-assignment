package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/**
 * Use case: Archive an existing active Warehouse.
 *
 * <p>Archiving is a soft-delete: the {@code archivedAt} timestamp is set and the warehouse is
 * excluded from all active-warehouse queries going forward. Its history is preserved.
 */
@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ArchiveWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  public void archive(Warehouse warehouse) {
    LOGGER.infof("Archiving warehouse: businessUnitCode=%s", warehouse.businessUnitCode);

    Warehouse existing = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseValidationException(
              "No active warehouse found with business unit code '" + warehouse.businessUnitCode + "'.");
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    LOGGER.infof("Warehouse '%s' archived successfully.", warehouse.businessUnitCode);
  }
}
