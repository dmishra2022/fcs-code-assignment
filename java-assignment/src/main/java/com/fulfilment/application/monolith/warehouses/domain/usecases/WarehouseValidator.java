package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseValidator {

    public void validateCreation(Warehouse warehouse, WarehouseStore warehouseStore,
            LocationResolver locationResolver) {
        // 1. Business Unit Code uniqueness
        if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
            throw new WarehouseValidationException(
                    "A warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists.");
        }

        validateBasicRules(warehouse, locationResolver);

        // 3. Max warehouses check at location
        Location location = locationResolver.resolveByIdentifier(warehouse.location);
        List<Warehouse> activeAtLocation = warehouseStore.findActiveByLocation(warehouse.location);
        if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
            throw new WarehouseValidationException(
                    "Location '" + warehouse.location + "' has reached the maximum number of warehouses ("
                            + location.maxNumberOfWarehouses + ").");
        }
    }

    public void validateReplacement(Warehouse newWarehouse, Warehouse existing, LocationResolver locationResolver) {
        validateBasicRules(newWarehouse, locationResolver);

        // 3. New capacity must accommodate the old stock
        if (existing.stock != null && newWarehouse.capacity < existing.stock) {
            throw new WarehouseValidationException(
                    "New warehouse capacity " + newWarehouse.capacity
                            + " cannot accommodate the existing stock of " + existing.stock + ".");
        }

        // 4. New stock must match the old warehouse stock
        if (newWarehouse.stock != null && !newWarehouse.stock.equals(existing.stock)) {
            throw new WarehouseValidationException(
                    "New warehouse stock " + newWarehouse.stock
                            + " must match the current stock of the warehouse being replaced: " + existing.stock + ".");
        }
    }

    private void validateBasicRules(Warehouse warehouse, LocationResolver locationResolver) {
        // 2. Location must be valid
        Location location = locationResolver.resolveByIdentifier(warehouse.location);
        if (location == null) {
            throw new WarehouseValidationException(
                    "Location '" + warehouse.location + "' does not exist.");
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
    }
}
