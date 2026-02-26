package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Use case: Create a new Warehouse.
 *
 * <p>
 * Business rules enforced:
 * <ol>
 * <li>Business Unit Code must be unique (no active warehouse with same
 * BUC).</li>
 * <li>Location must exist.</li>
 * <li>Number of active warehouses at that location must be below the location's
 * maximum.</li>
 * <li>Requested capacity must not exceed the location's max capacity.</li>
 * <li>Stock must not exceed capacity.</li>
 * </ol>
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;
  private final WarehouseValidator warehouseValidator;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore,
      LocationResolver locationResolver,
      WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void create(Warehouse warehouse) {
    LOGGER.infof(
        "Creating warehouse: businessUnitCode=%s, location=%s",
        warehouse.businessUnitCode, warehouse.location);

    warehouseValidator.validateCreation(warehouse, warehouseStore, locationResolver);

    warehouseStore.create(warehouse);
    LOGGER.infof("Warehouse '%s' created successfully.", warehouse.businessUnitCode);
  }
}
