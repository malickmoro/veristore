package com.theplutushome.veristore.view;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.dto.ProductCardDTO;
import com.theplutushome.veristore.entity.Product;
import com.theplutushome.veristore.repo.ProductRepo;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ProductView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<ProductCardDTO> items = new ArrayList<>();
    private String country;
    private String category;
    private String maxPrice;
    private int page;
    private final int size = 12;
    private int pages;
    private boolean hasPrev;
    private boolean hasNext;

    @Inject
    ProductRepo productRepo;

    @Inject
    GlobalView globalView;

    @PostConstruct
    public void init() {
        loadItems();
    }

    public void applyFilters() {
        page = 0;
        loadItems();
    }

    public void next() {
        if (hasNext) {
            page++;
            loadItems();
        }
    }

    public void prev() {
        if (hasPrev) {
            page--;
            loadItems();
        }
    }

    public List<ProductCardDTO> getItems() {
        return items;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(String maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getPages() {
        return pages;
    }

    public boolean isHasPrev() {
        return hasPrev;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    private void loadItems() {
        Long maxPriceMinor = parseMaxPrice();
        long total = productRepo.countSearch(country, category, maxPriceMinor);
        pages = size > 0 ? (int) Math.ceil(total / (double) size) : 0;
        if (pages > 0 && page >= pages) {
            page = pages - 1;
        }
        if (pages == 0) {
            page = 0;
        }
        items.clear();
        for (Product product : productRepo.search(country, category, maxPriceMinor, page, size)) {
            items.add(toCard(product));
        }
        hasPrev = page > 0;
        hasNext = pages > 0 && (page + 1) < pages;
    }

    private Long parseMaxPrice() {
        if (maxPrice == null || maxPrice.isBlank()) {
            return null;
        }
        try {
            String normalized = maxPrice.replace(",", ".");
            if (normalized.contains(".")) {
                String[] parts = normalized.split("\\.", 2);
                long whole = Long.parseLong(parts[0].isBlank() ? "0" : parts[0]);
                String fractionPart = parts.length > 1 ? parts[1] : "";
                fractionPart = (fractionPart + "00").substring(0, 2);
                long fraction = Long.parseLong(fractionPart);
                long minor = whole * 100 + fraction;
                return minor < 0 ? null : minor;
            }
            long minor = Long.parseLong(normalized) * 100;
            return minor < 0 ? null : minor;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private ProductCardDTO toCard(Product product) {
        ProductCardDTO dto = new ProductCardDTO();
        dto.setId(product.getId());
        dto.setSku(product.getSku());
        dto.setName(product.getName());
        dto.setShortDesc(Optional.ofNullable(product.getShortDesc()).orElse(""));
        dto.setCountry(Optional.ofNullable(product.getCountry()).orElse(""));
        dto.setCategory(Optional.ofNullable(product.getCategory()).orElse(""));
        dto.setThumbnailUrl(Optional.ofNullable(product.getThumbnailUrl())
                .filter(value -> !value.isBlank())
                .orElse(Optional.ofNullable(product.getImageUrl()).orElse("")));
        dto.setPriceFormatted(globalView.formatMoney(product.getPriceMinor(), product.getCurrency()));
        return dto;
    }
}
