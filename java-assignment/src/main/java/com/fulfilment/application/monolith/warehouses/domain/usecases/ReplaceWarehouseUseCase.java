package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/**
 * Use case: Replace an existing active Warehouse.
 *
 * <p>
 * The replacement operation archives the current active warehouse and
 * immediately creates the
 * new warehouse reusing the same Business Unit Code, preserving the unit's
 * history.
 *
 * <p>
 * Additional validations over creation:
 * <ol>
 * <li>The warehouse to replace must exist and be active.</li>
 * <li>New warehouse location must be valid.</li>
 * <li>New warehouse capacity must be able to accommodate the stock of the old
 * warehouse.</li>
 * <li>New warehouse stock must match the stock of the old warehouse.</li>
 * <li>New capacity must not exceed location's max capacity.</li>
 * </ol>
 */
@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidator warehouseValidator;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    LOGGER.infof("Replacing warehouse with businessUnitCode=%s", newWarehouse.businessUnitCode);

    // 1. Existing warehouse must exist
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WarehouseValidationException(
          "No active warehouse found with business unit code '"
              + newWarehouse.businessUnitCode
              + "'.");
    }

    warehouseValidator.validateReplacement(newWarehouse, existing, locationResolver);

    // Archive the old warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);
    LOGGER.infof("Archived warehouse '%s'.", existing.businessUnitCode);

    // Create the new warehouse (reusing the business unit code)
    newWarehouse.businessUnitCode = existing.businessUnitCode;
    warehouseStore.create(newWarehouse);
    LOGGER.infof("New warehouse '%s' created successfully.", newWarehouse.businessUnitCode);
  }
}
