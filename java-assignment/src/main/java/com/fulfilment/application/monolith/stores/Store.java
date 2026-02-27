package com.fulfilment.application.monolith.stores;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@Cacheable
public class Store extends PanacheEntity {

  @jakarta.persistence.ManyToMany
  @jakarta.persistence.JoinTable(name = "store_warehouse", joinColumns = @jakarta.persistence.JoinColumn(name = "store_id"), inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "warehouse_id"))
  @com.fasterxml.jackson.annotation.JsonIgnore
  public java.util.List<com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse> fulfilmentUnits = new java.util.ArrayList<>();

  @Column(length = 40, unique = true)
  public String name;

  public int quantityProductsInStock;

  public Store() {
  }

  public Store(String name) {
    this.name = name;
  }
}
