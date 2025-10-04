package com.theplutushome.veristore.view;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.theplutushome.veristore.dto.CartLineDTO;
import com.theplutushome.veristore.entity.Product;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@SessionScoped
public class CartView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<CartLineDTO> lines = new ArrayList<>();

    @Inject
    GlobalView globalView;

    public List<CartLineDTO> getLines() {
        return lines;
    }

    public void add(Product product, int qty) {
        if (product == null || qty <= 0) {
            return;
        }
        CartLineDTO line = findLine(product.getSku());
        if (line == null) {
            line = new CartLineDTO();
            line.setSku(product.getSku());
            line.setName(product.getName());
            line.setCurrency(Optional.ofNullable(product.getCurrency()).orElse(globalView.getCurrency()));
            line.setUnitPriceMinor(product.getPriceMinor());
            lines.add(line);
        }
        line.setQty(Math.max(1, line.getQty() + qty));
        refreshLine(line);
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Added to cart", product.getName() + " x" + line.getQty()));
        }
    }

    public void update(CartLineDTO updatedLine) {
        if (updatedLine == null) {
            return;
        }
        CartLineDTO existing = findLine(updatedLine.getSku());
        if (existing == null) {
            return;
        }
        int newQty = Math.max(0, updatedLine.getQty());
        if (newQty == 0) {
            remove(existing);
            return;
        }
        existing.setQty(newQty);
        refreshLine(existing);
    }

    public void remove(CartLineDTO line) {
        if (line == null) {
            return;
        }
        lines.removeIf(existing -> existing.getSku().equalsIgnoreCase(line.getSku()));
    }

    public void empty() {
        lines.clear();
    }

    public void recalculate() {
        lines.removeIf(line -> line.getQty() <= 0);
        lines.forEach(line -> {
            line.setQty(Math.max(1, line.getQty()));
            refreshLine(line);
        });
    }

    public long subtotal() {
        return lines.stream().mapToLong(CartLineDTO::getTotalMinor).sum();
    }

    public long fees() {
        long subtotalMinor = subtotal();
        if (subtotalMinor <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(subtotalMinor)
                .multiply(BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    public long total() {
        return subtotal() + fees();
    }

    public String subtotalFormatted() {
        return globalView.formatMoney(subtotal(), getCurrencyCode());
    }

    public String feesFormatted() {
        return globalView.formatMoney(fees(), getCurrencyCode());
    }

    public String totalFormatted() {
        return globalView.formatMoney(total(), getCurrencyCode());
    }

    public String getCurrencyCode() {
        if (lines.isEmpty()) {
            return globalView.getCurrency();
        }
        return Optional.ofNullable(lines.get(0).getCurrency()).orElse(globalView.getCurrency());
    }

    private CartLineDTO findLine(String sku) {
        if (sku == null) {
            return null;
        }
        return lines.stream()
                .filter(line -> sku.equalsIgnoreCase(line.getSku()))
                .findFirst()
                .orElse(null);
    }

    private void refreshLine(CartLineDTO line) {
        if (line.getCurrency() == null || line.getCurrency().isBlank()) {
            line.setCurrency(globalView.getCurrency());
        }
        long totalMinor = line.getUnitPriceMinor() * line.getQty();
        line.setTotalMinor(totalMinor);
        line.setPriceFormatted(globalView.formatMoney(line.getUnitPriceMinor(), line.getCurrency()));
        line.setTotalFormatted(globalView.formatMoney(totalMinor, line.getCurrency()));
    }

    public void clear() {
        lines = new ArrayList<>();
    }

}
