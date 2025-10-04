package com.theplutushome.veristore.repo;

import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.entity.SecretCode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SecretCodeRepo {

    @PersistenceContext
    EntityManager entityManager;

    public Optional<SecretCode> findById(Long id) {
        return Optional.ofNullable(entityManager.find(SecretCode.class, id));
    }

    public List<SecretCode> findByOrderId(Long orderId) {
        return entityManager.createQuery(
                        "SELECT sc FROM SecretCode sc WHERE sc.orderId = :orderId ORDER BY sc.createdAt", SecretCode.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    @Transactional
    public SecretCode save(SecretCode secretCode) {
        if (secretCode.getId() == null) {
            entityManager.persist(secretCode);
            return secretCode;
        }
        return entityManager.merge(secretCode);
    }

    @Transactional
    public void delete(SecretCode secretCode) {
        SecretCode managed = secretCode;
        if (!entityManager.contains(secretCode)) {
            managed = entityManager.merge(secretCode);
        }
        entityManager.remove(managed);
    }
}
