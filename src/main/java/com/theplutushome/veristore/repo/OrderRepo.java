package com.theplutushome.veristore.repo;

import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.entity.Order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderRepo {

    @PersistenceContext
    EntityManager entityManager;

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Order.class, id));
    }

    public Optional<Order> findByPublicId(String publicId) {
        TypedQuery<Order> query = entityManager.createQuery(
                "SELECT o FROM Order o WHERE LOWER(o.publicId) = LOWER(:publicId)", Order.class);
        query.setParameter("publicId", publicId);
        return query.getResultStream().findFirst();
    }

    public List<Order> findAll() {
        return entityManager.createQuery("SELECT o FROM Order o ORDER BY o.createdAt DESC", Order.class)
                .getResultList();
    }

    @Transactional
    public Order save(Order order) {
        if (order.getId() == null) {
            entityManager.persist(order);
            return order;
        }
        return entityManager.merge(order);
    }

    @Transactional
    public void delete(Order order) {
        Order managed = order;
        if (!entityManager.contains(order)) {
            managed = entityManager.merge(order);
        }
        entityManager.remove(managed);
    }
}
