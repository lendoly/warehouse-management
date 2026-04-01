package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;


@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Transactional
  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();

    try {
      createWarehouseOperation.create(warehouse);
    } catch (IllegalArgumentException e) {
      throw new jakarta.ws.rs.WebApplicationException(e.getMessage(), 400);
    }

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new jakarta.ws.rs.WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }
    return toWarehouseResponse(warehouse);
  }

  @Transactional
  @Override
  public void archiveAWarehouseUnitByID(String id) {
    var warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) {
      throw new jakarta.ws.rs.WebApplicationException("Warehouse with id " + id + " does not exist.", 404);
    }
    warehouse.archivedAt = LocalDateTime.now();
    archiveWarehouseOperation.archive(warehouse);
  }

  @Transactional
  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var existing = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    if (existing == null) {
      throw new jakarta.ws.rs.WebApplicationException("Warehouse with id " + businessUnitCode + " does not exist.", 404);
    }
    existing.location = data.getLocation();
    existing.capacity = data.getCapacity();
    existing.stock = data.getStock();
    replaceWarehouseOperation.replace(existing);
    return toWarehouseResponse(existing);
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
