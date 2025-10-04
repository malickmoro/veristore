package com.theplutushome.veristore.repo;

import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.entity.OrderLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderLineRepo {

    @PersistenceContext
    EntityManager entityManager;

    public Optional<OrderLine> findById(Long id) {
        return Optional.ofNullable(entityManager.find(OrderLine.class, id));
    }

    public List<OrderLine> findByOrderId(Long orderId) {
        return entityManager.createQuery(
                        "SELECT ol FROM OrderLine ol WHERE ol.orderId = :orderId ORDER BY ol.id", OrderLine.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    @Transactional
    public OrderLine save(OrderLine orderLine) {
        if (orderLine.getId() == null) {
            entityManager.persist(orderLine);
            return orderLine;
        }
        return entityManager.merge(orderLine);
    }

    @Transactional
    public void delete(OrderLine orderLine) {
        OrderLine managed = orderLine;
        if (!entityManager.contains(orderLine)) {
            managed = entityManager.merge(orderLine);
        }
        entityManager.remove(managed);
    }
}
