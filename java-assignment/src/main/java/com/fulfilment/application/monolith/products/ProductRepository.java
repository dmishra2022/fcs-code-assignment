package com.fulfilment.application.monolith.products;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    @Inject
    EntityManager em;

    public long countByWarehouseId(Long warehouseId) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM Product p JOIN p.fulfilmentUnits w WHERE w.id = :wid", Long.class)
                .setParameter("wid", warehouseId)
                .getSingleResult();
    }
}
