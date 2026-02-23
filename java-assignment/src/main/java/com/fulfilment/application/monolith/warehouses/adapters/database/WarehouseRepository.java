package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * JPA/Panache-backed implementation of {@link WarehouseStore}.
 * Handles all database interactions for the Warehouse aggregate.
 */
@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class.getName());

  @Override
  public List<Warehouse> getAll() {
    // Return only active warehouses (archivedAt is null)
    return find("archivedAt IS NULL").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public List<Warehouse> findActiveByLocation(String locationIdentifier) {
    return find("location = ?1 AND archivedAt IS NULL", locationIdentifier)
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    LOGGER.infof("Creating warehouse with businessUnitCode=%s", warehouse.businessUnitCode);
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = LocalDateTime.now();
    dbWarehouse.archivedAt = null;
    persist(dbWarehouse);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    LOGGER.infof("Updating warehouse with businessUnitCode=%s", warehouse.businessUnitCode);
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 AND archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse not found for update: " + warehouse.businessUnitCode);
    }
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    LOGGER.infof("Removing warehouse with businessUnitCode=%s", warehouse.businessUnitCode);
    DbWarehouse dbWarehouse =
        find("businessUnitCode = ?1 AND archivedAt IS NULL", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse != null) {
      delete(dbWarehouse);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return find("businessUnitCode = ?1 AND archivedAt IS NULL", buCode)
        .firstResultOptional()
        .map(DbWarehouse::toWarehouse)
        .orElse(null);
  }
}
