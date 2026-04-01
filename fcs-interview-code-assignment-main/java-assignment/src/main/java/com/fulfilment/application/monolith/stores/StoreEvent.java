package com.fulfilment.application.monolith.stores;

public class StoreEvent {

  public enum Operation {
    CREATE,
    UPDATE
  }

  public final Store store;
  public final Operation operation;

  public StoreEvent(Store store, Operation operation) {
    this.store = store;
    this.operation = operation;
  }
}
