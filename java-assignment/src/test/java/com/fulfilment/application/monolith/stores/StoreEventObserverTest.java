package com.fulfilment.application.monolith.stores;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreEventObserver Tests")
class StoreEventObserverTest {

  @Mock private LegacyStoreManagerGateway legacyGateway;

  private StoreEventObserver observer;

  @BeforeEach
  void setUp() {
    observer = new StoreEventObserver();
    // Inject mock via reflection since @Inject is not wired in unit tests
    try {
      var field = StoreEventObserver.class.getDeclaredField("legacyStoreManagerGateway");
      field.setAccessible(true);
      field.set(observer, legacyGateway);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("CREATED event invokes createStoreOnLegacySystem")
  void onStoreCreated_createdEvent_invokesCreate() {
    Store store = new Store("TEST-STORE");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.CREATED);

    observer.onStoreCreated(event);

    verify(legacyGateway).createStoreOnLegacySystem(store);
    verify(legacyGateway, never()).updateStoreOnLegacySystem(any());
  }

  @Test
  @DisplayName("UPDATED event invokes updateStoreOnLegacySystem")
  void onStoreCreated_updatedEvent_invokesUpdate() {
    Store store = new Store("TEST-STORE");
    StoreEvent event = new StoreEvent(store, StoreEvent.Type.UPDATED);

    observer.onStoreCreated(event);

    verify(legacyGateway).updateStoreOnLegacySystem(store);
    verify(legacyGateway, never()).createStoreOnLegacySystem(any());
  }
}
