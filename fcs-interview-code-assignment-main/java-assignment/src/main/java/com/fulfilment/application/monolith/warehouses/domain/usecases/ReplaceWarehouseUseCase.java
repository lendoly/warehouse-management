package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.Objects;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findWarehouseById(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new IllegalArgumentException(
          "Warehouse with businessUnitCode " + newWarehouse.businessUnitCode + " not found.");
    }

    // Stock of the new warehouse must match the stock being transferred from the existing one
    if (!Objects.equals(newWarehouse.stock, existing.stock)) {
      throw new IllegalArgumentException(
          "New warehouse stock (" + newWarehouse.stock
          + ") must match the stock of the replaced warehouse (" + existing.stock + ").");
    }

    // New warehouse capacity must be able to accommodate the transferred stock
    if (existing.stock != null && newWarehouse.capacity != null
        && newWarehouse.capacity < existing.stock) {
      throw new IllegalArgumentException(
          "New warehouse capacity (" + newWarehouse.capacity
          + ") cannot accommodate the existing stock (" + existing.stock + ").");
    }

    // Validate location, excluding the being-replaced warehouse from the capacity/count sums
    warehouseValidator.validateLocation(newWarehouse, newWarehouse.businessUnitCode);

    // Step 1: archive the existing warehouse
    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    // Step 2: create the new warehouse with the same businessUnitCode
    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
