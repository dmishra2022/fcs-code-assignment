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
 * <p>Business rules enforced:
 * <ol>
 *   <li>Business Unit Code must be unique (no active warehouse with same BUC).</li>
 *   <li>Location must exist.</li>
 *   <li>Number of active warehouses at that location must be below the location's maximum.</li>
 *   <li>Requested capacity must not exceed the location's max capacity.</li>
 *   <li>Stock must not exceed capacity.</li>
 * </ol>
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class.getName());

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    LOGGER.infof("Creating warehouse: businessUnitCode=%s, location=%s",
            warehouse.businessUnitCode, warehouse.location);

    // 1. Business Unit Code uniqueness
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseValidationException(
              "A warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists.");
    }

    // 2. Location must be valid
    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WarehouseValidationException(
              "Location '" + warehouse.location + "' does not exist.");
    }

    // 3. Max warehouses check at location
    List<Warehouse> activeAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
              "Location '" + warehouse.location + "' has reached the maximum number of warehouses ("
                      + location.maxNumberOfWarehouses + ").");
    }

    // 4. Capacity must not exceed location max capacity
    if (warehouse.capacity > location.maxCapacity) {
      throw new WarehouseValidationException(
              "Requested capacity " + warehouse.capacity + " exceeds the maximum allowed capacity "
                      + location.maxCapacity + " for location '" + warehouse.location + "'.");
    }

    // 5. Stock must not exceed capacity
    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new WarehouseValidationException(
              "Stock " + warehouse.stock + " exceeds warehouse capacity " + warehouse.capacity + ".");
    }

    warehouseStore.create(warehouse);
    LOGGER.infof("Warehouse '%s' created successfully.", warehouse.businessUnitCode);
  }
}
