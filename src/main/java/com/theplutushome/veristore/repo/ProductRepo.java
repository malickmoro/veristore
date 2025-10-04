package com.theplutushome.veristore.repo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.entity.Product;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ProductRepo {

    @PersistenceContext
    EntityManager entityManager;

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }

    public Optional<Product> findBySku(String sku) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);
        cq.select(root).where(cb.equal(cb.lower(root.get("sku")), sku.toLowerCase()));

        List<Product> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();
        return results.stream().findFirst();
    }

    public List<Product> search(String country, String category, Long maxPriceMinor, int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        Predicate[] filters = buildFilters(cb, root, country, category, maxPriceMinor);
        cq.select(root).where(filters).orderBy(cb.asc(root.get("name")));

        TypedQuery<Product> query = entityManager.createQuery(cq);
        if (size > 0) {
            int firstResult = Math.max(page, 0) * size;
            query.setFirstResult(firstResult);
            query.setMaxResults(size);
        }

        return query.getResultList();
    }

    public long countSearch(String country, String category, Long maxPriceMinor) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Product> root = cq.from(Product.class);
        Predicate[] filters = buildFilters(cb, root, country, category, maxPriceMinor);
        cq.select(cb.count(root)).where(filters);
        return entityManager.createQuery(cq).getSingleResult();
    }

    @Transactional
    public Product save(Product product) {
        if (product.getId() == null) {
            entityManager.persist(product);
            return product;
        }
        return entityManager.merge(product);
    }

    @Transactional
    public void delete(Product product) {
        Product managed = product;
        if (!entityManager.contains(product)) {
            managed = entityManager.merge(product);
        }
        entityManager.remove(managed);
    }

    private Predicate[] buildFilters(CriteriaBuilder cb, Root<Product> root, String country, String category,
            Long maxPriceMinor) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("active")));

        if (country != null && !country.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("country")), country.toLowerCase()));
        }

        if (category != null && !category.isBlank()) {
            predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }

        if (maxPriceMinor != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("priceMinor"), maxPriceMinor));
        }
        return predicates.toArray(new Predicate[0]);
    }
}
