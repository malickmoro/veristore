package com.theplutushome.veristore.view;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

import com.theplutushome.veristore.dto.ProductDetailDTO;
import com.theplutushome.veristore.entity.Product;
import com.theplutushome.veristore.repo.ProductRepo;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ProductDetailView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private ProductDetailDTO item;
    private Product product;
    private int qty = 1;
    private boolean acceptedTos;

    @Inject
    ProductRepo productRepo;

    @Inject
    CartView cartView;

    @Inject
    GlobalView globalView;

    @PostConstruct
    public void init() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return;
        }
        String sku = facesContext.getExternalContext().getRequestParameterMap().getOrDefault("sku", "");
        if (sku.isBlank()) {
            return;
        }
        productRepo.findBySku(sku).ifPresent(this::loadProduct);
    }

    public void addToCart() {
        if (product == null) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (!acceptedTos) {
            if (facesContext != null) {
                facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                        "Please accept the terms to continue", null));
            }
            return;
        }
        int quantity = Math.max(1, qty);
        cartView.add(product, quantity);
        qty = 1;
        acceptedTos = false;
    }

    public ProductDetailDTO getItem() {
        return item;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public boolean isAcceptedTos() {
        return acceptedTos;
    }

    public void setAcceptedTos(boolean acceptedTos) {
        this.acceptedTos = acceptedTos;
    }

    private void loadProduct(Product found) {
        this.product = found;
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(found.getId());
        dto.setSku(found.getSku());
        dto.setName(found.getName());
        dto.setShortDesc(Optional.ofNullable(found.getShortDesc()).orElse(""));
        dto.setLongDesc(Optional.ofNullable(found.getLongDesc()).orElse(""));
        dto.setCountry(Optional.ofNullable(found.getCountry()).orElse(""));
        dto.setCategory(Optional.ofNullable(found.getCategory()).orElse(""));
        dto.setImageUrl(Optional.ofNullable(found.getImageUrl()).orElse(found.getThumbnailUrl()));
        dto.setCurrency(Optional.ofNullable(found.getCurrency()).orElse(globalView.getCurrency()));
        dto.setPriceFormatted(globalView.formatMoney(found.getPriceMinor(), dto.getCurrency()));
        this.item = dto;
    }
}
