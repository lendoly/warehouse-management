package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Cacheable
@Table(
    name = "fulfillment_unit",
    uniqueConstraints = @UniqueConstraint(columnNames = {"storeId", "productId", "warehouseBusinessUnitCode"})
)
public class FulfillmentUnit extends PanacheEntity {

  public Long storeId;

  public Long productId;

  @Column(nullable = false)
  public String warehouseBusinessUnitCode;
}
