package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LocationGateway Tests")
class LocationGatewayTest {

  private LocationGateway gateway;

  @BeforeEach
  void setUp() {
    gateway = new LocationGateway();
  }

  @Test
  @DisplayName("resolveByIdentifier returns location when identifier matches")
  void resolveByIdentifier_existingLocation_returnsLocation() {
    Location result = gateway.resolveByIdentifier("AMSTERDAM-001");

    assertNotNull(result);
    assertEquals("AMSTERDAM-001", result.identification);
    assertEquals(5, result.maxNumberOfWarehouses);
    assertEquals(100, result.maxCapacity);
  }

  @Test
  @DisplayName("resolveByIdentifier returns null for unknown identifier")
  void resolveByIdentifier_unknownLocation_returnsNull() {
    Location result = gateway.resolveByIdentifier("UNKNOWN-999");
    assertNull(result);
  }

  @Test
  @DisplayName("resolveByIdentifier returns null for null input")
  void resolveByIdentifier_nullInput_returnsNull() {
    Location result = gateway.resolveByIdentifier(null);
    assertNull(result);
  }

  @Test
  @DisplayName("resolveByIdentifier returns null for blank input")
  void resolveByIdentifier_blankInput_returnsNull() {
    Location result = gateway.resolveByIdentifier("  ");
    assertNull(result);
  }

  @Test
  @DisplayName("resolveByIdentifier correctly returns ZWOLLE-001 attributes")
  void resolveByIdentifier_zwolle001_returnsCorrectAttributes() {
    Location result = gateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(result);
    assertEquals(1, result.maxNumberOfWarehouses);
    assertEquals(40, result.maxCapacity);
  }

  @Test
  @DisplayName("resolveByIdentifier correctly returns ZWOLLE-002 attributes")
  void resolveByIdentifier_zwolle002_returnsCorrectAttributes() {
    Location result = gateway.resolveByIdentifier("ZWOLLE-002");

    assertNotNull(result);
    assertEquals(2, result.maxNumberOfWarehouses);
    assertEquals(50, result.maxCapacity);
  }

  @Test
  @DisplayName("resolveByIdentifier is case-sensitive")
  void resolveByIdentifier_caseMismatch_returnsNull() {
    Location result = gateway.resolveByIdentifier("amsterdam-001");
    assertNull(result);
  }
}
