package com.fulfilment.application.monolith.warehouses.domain.models;

import java.time.LocalDateTime;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;

public class Warehouse {

  // unique identifier
  public String businessUnitCode;

  public String location;

  public Integer capacity;

  public Integer stock;

  public LocalDateTime createdAt;

  public LocalDateTime archivedAt;

  public DbWarehouse toDbWarehouse() {
    var dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = this.businessUnitCode;
    dbWarehouse.location = this.location;
    dbWarehouse.capacity = this.capacity;
    dbWarehouse.stock = this.stock;
    dbWarehouse.createdAt = this.createdAt;
    dbWarehouse.archivedAt = this.archivedAt;
    return dbWarehouse;
  }
}
