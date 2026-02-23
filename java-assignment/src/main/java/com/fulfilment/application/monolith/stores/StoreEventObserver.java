package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Observes {@link StoreEvent} fired within a transaction and delegates to the
 * {@link LegacyStoreManagerGateway} only after the transaction has been
 * successfully committed. This guarantees the downstream legacy system receives
 * confirmed, durable data and never sees a rolled-back partial write.
 */
@ApplicationScoped
public class StoreEventObserver {

  private static final Logger LOGGER = Logger.getLogger(StoreEventObserver.class.getName());

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreEvent event) {
    LOGGER.infof(
        "Transaction committed successfully. Propagating store event [type=%s, store=%s] to legacy system.",
        event.getType(), event.getStore().name);

    switch (event.getType()) {
      case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.getStore());
      case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.getStore());
    }
  }
}
