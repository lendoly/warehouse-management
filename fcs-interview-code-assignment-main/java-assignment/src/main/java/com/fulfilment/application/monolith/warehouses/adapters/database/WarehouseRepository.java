package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    var dbWarehouse = warehouse.toDbWarehouse();
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse =
        this.find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
            .firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException("Warehouse not found: " + warehouse.businessUnitCode);
    }
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = this.find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException("Warehouse not found: " + warehouse.businessUnitCode);
    }
    this.delete(dbWarehouse);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse =
        this.find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    if (dbWarehouse == null) {
      return null;
    }
    return dbWarehouse.toWarehouse();
  }

  @Override
  public long countActiveByLocation(String location) {
    return this.count("location = ?1 and archivedAt is null", location);
  }
}
