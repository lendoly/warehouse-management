package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new IllegalArgumentException(
          "Warehouse with businessUnitCode " + warehouse.businessUnitCode + " already exists.");
    }

    var location = locationResolver.resolveByIdentifier(warehouse.location);

    long activeCount = warehouseStore.countActiveByLocation(warehouse.location);
    if (activeCount >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Location " + warehouse.location + " has reached the maximum number of warehouses (" + location.maxNumberOfWarehouses + ").");
    }

    int usedCapacity = warehouseStore.getAll().stream()
        .filter(w -> warehouse.location.equals(w.location) && w.archivedAt == null)
        .mapToInt(w -> w.capacity != null ? w.capacity : 0)
        .sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Location " + warehouse.location + " would exceed maximum capacity of " + location.maxCapacity
          + " (current: " + usedCapacity + ", requested: " + warehouse.capacity + ").");
    }

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
