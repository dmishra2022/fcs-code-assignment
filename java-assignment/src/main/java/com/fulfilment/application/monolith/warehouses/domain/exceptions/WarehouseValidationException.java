package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Thrown when a warehouse operation violates a business rule or constraint.
 * Translates to HTTP 400 Bad Request at the API layer.
 */
public class WarehouseValidationException extends RuntimeException {

  public WarehouseValidationException(String message) {
    super(message);
  }

  public WarehouseValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
