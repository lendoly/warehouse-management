package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class StoreEventListener {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    if (event.operation == StoreEvent.Operation.CREATE) {
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
    } else {
      legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
    }
  }
}
