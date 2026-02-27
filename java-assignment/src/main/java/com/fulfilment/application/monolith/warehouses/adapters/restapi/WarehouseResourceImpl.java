package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * REST adapter implementing the generated {@link WarehouseResource} interface.
 *
 * <p>
 * Delegates all business logic to the use cases, keeping this class purely
 * concerned with HTTP protocol concerns (request/response translation, error
 * mapping).
 */
@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class.getName());

  @Inject
  private WarehouseRepository warehouseRepository;
  @Inject
  private CreateWarehouseOperation createWarehouseOperation;
  @Inject
  private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject
  private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toApiWarehouse).toList();
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(
      @NotNull com.warehouse.api.beans.Warehouse data) {
    LOGGER.infof("POST /warehouse - Creating warehouse: businessUnitCode=%s", data.getBusinessUnitCode());

    try {
      Warehouse domainWarehouse = toDomainWarehouse(data);
      createWarehouseOperation.create(domainWarehouse);

      // Reload from store to get the persisted state (e.g., createdAt)
      Warehouse created = warehouseRepository.findByBusinessUnitCode(domainWarehouse.businessUnitCode);
      return toApiWarehouse(created);
    } catch (WarehouseValidationException e) {
      LOGGER.warnf("Warehouse creation validation failed: %s", e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    LOGGER.infof("GET /warehouse/%s", id);

    Warehouse warehouse = null;
    try {
      warehouse = warehouseRepository.findWarehouseById(Long.parseLong(id));
    } catch (NumberFormatException e) {
      // Fallback: treat id as businessUnitCode
      warehouse = warehouseRepository.findByBusinessUnitCode(id);
    }

    if (warehouse == null) {
      throw new NotFoundException("Warehouse with id or code '" + id + "' not found.");
    }
    return toApiWarehouse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    LOGGER.infof("DELETE /warehouse/%s - Archiving warehouse", id);

    Warehouse warehouse = null;
    try {
      warehouse = warehouseRepository.findWarehouseById(Long.parseLong(id));
    } catch (NumberFormatException e) {
      // Fallback: treat id as businessUnitCode
      warehouse = warehouseRepository.findByBusinessUnitCode(id);
    }

    if (warehouse == null) {
      throw new NotFoundException("Warehouse with id or code '" + id + "' not found.");
    }

    try {
      archiveWarehouseOperation.archive(warehouse);
    } catch (WarehouseValidationException e) {
      LOGGER.warnf("Warehouse archive failed: %s", e.getMessage());
      throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    LOGGER.infof("POST /warehouse/%s/replacement - Replacing warehouse", businessUnitCode);

    try {
      Warehouse newWarehouse = toDomainWarehouse(data);
      newWarehouse.businessUnitCode = businessUnitCode;
      replaceWarehouseOperation.replace(newWarehouse);

      Warehouse replaced = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      return toApiWarehouse(replaced);
    } catch (WarehouseValidationException e) {
      LOGGER.warnf("Warehouse replacement validation failed: %s", e.getMessage());
      throw new WebApplicationException(
          e.getMessage(),
          e.getMessage().contains("not found") || e.getMessage().contains("No active")
              ? Response.Status.NOT_FOUND
              : Response.Status.BAD_REQUEST);
    }
  }

  // ─── Mapping helpers ──────────────────────────────────────────────────────

  private com.warehouse.api.beans.Warehouse toApiWarehouse(Warehouse warehouse) {
    if (warehouse == null)
      return null;
    var response = new com.warehouse.api.beans.Warehouse();
    response.setId(warehouse.id != null ? warehouse.id.toString() : null);
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }

  private Warehouse toDomainWarehouse(com.warehouse.api.beans.Warehouse apiWarehouse) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = apiWarehouse.getBusinessUnitCode();
    warehouse.location = apiWarehouse.getLocation();
    warehouse.capacity = apiWarehouse.getCapacity();
    warehouse.stock = apiWarehouse.getStock() != null ? apiWarehouse.getStock() : 0;
    return warehouse;
  }
}
