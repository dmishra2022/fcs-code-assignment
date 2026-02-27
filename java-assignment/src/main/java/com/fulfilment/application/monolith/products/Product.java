package com.fulfilment.application.monolith.products;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
@Cacheable
public class Product {

  @Id
  @GeneratedValue
  public Long id;

  @jakarta.persistence.ManyToMany
  @jakarta.persistence.JoinTable(name = "product_warehouse", joinColumns = @jakarta.persistence.JoinColumn(name = "product_id"), inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "warehouse_id"))
  @com.fasterxml.jackson.annotation.JsonIgnore
  public java.util.List<com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse> fulfilmentUnits = new java.util.ArrayList<>();

  @Column(length = 40, unique = true)
  public String name;

  @Column(nullable = true)
  public String description;

  @Column(precision = 10, scale = 2, nullable = true)
  public BigDecimal price;

  public int stock;

  public Product() {
  }

  public Product(String name) {
    this.name = name;
  }
}
