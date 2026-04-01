package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WarehouseValidator {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  public void validateWarehouse(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new IllegalArgumentException(
          "Warehouse with businessUnitCode " + warehouse.businessUnitCode + " already exists.");
    }

    if (warehouse.stock != null && warehouse.capacity != null && warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException(
          "Stock (" + warehouse.stock + ") cannot be greater than capacity (" + warehouse.capacity + ").");
    }
  }

  /**
   * Validates location existence, warehouse count limit, and capacity limit.
   * Pass excludeBusinessUnitCode to exempt an existing warehouse from the capacity sum (used on replace).
   */
  public void validateLocation(Warehouse warehouse, String excludeBusinessUnitCode) {
    var location = locationResolver.resolveByIdentifier(warehouse.location);

    long activeCount = warehouseStore.countActiveByLocation(warehouse.location);
    if (activeCount >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Location " + warehouse.location + " has reached the maximum number of warehouses (" + location.maxNumberOfWarehouses + ").");
    }

    int usedCapacity = warehouseStore.getAll().stream()
        .filter(w -> warehouse.location.equals(w.location))
        .mapToInt(w -> w.capacity != null ? w.capacity : 0)
        .sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Location " + warehouse.location + " would exceed maximum capacity of " + location.maxCapacity
          + " (current: " + usedCapacity + ", requested: " + warehouse.capacity + ").");
    }
  }
}
