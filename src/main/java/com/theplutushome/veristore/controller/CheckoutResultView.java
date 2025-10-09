package com.theplutushome.veristore.controller;

import com.theplutushome.veristore.model.PaymentMode;
import com.theplutushome.veristore.model.Price;
import com.theplutushome.veristore.service.OrderStore;
import com.theplutushome.veristore.service.PricingService;
import com.theplutushome.veristore.util.Masker;
import com.theplutushome.veristore.util.VariantDescriptions;
import com.theplutushome.veristore.controller.CartView.CheckoutReference;
import com.theplutushome.veristore.controller.CartView.CheckoutResult;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class CheckoutResultView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    private CartView cartView;

    @Inject
    private OrderStore orderStore;

    @Inject
    private PricingService pricingService;

    private final List<ResultLine> lines = new ArrayList<>();
    private PaymentMode paymentMode;
    private boolean missing;
    private boolean loaded;

    public void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            missing = true;
            return;
        }
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> params = externalContext.getRequestParameterMap();
        String token = Optional.ofNullable(params.get("token")).filter(s -> !s.isBlank()).orElse(null);
        if (token == null) {
            missing = true;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Checkout session not found.", null));
            return;
        }
        CheckoutResult result = cartView.consumeCheckoutResult(token);
        if (result == null) {
            missing = true;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Checkout summary has expired.", null));
            return;
        }
        paymentMode = result.getPaymentMode();
        for (CheckoutReference reference : result.getReferences()) {
            switch (reference.getType()) {
                case ORDER -> orderStore.findOrder(reference.getReference()).ifPresent(order ->
                        lines.add(ResultLine.fromOrder(order, pricingService)));
                case INVOICE -> orderStore.findInvoice(reference.getReference()).ifPresent(invoice ->
                        lines.add(ResultLine.fromInvoice(invoice, pricingService)));
            }
        }
        if (lines.isEmpty()) {
            missing = true;
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No checkout records could be found.", null));
        }
    }

    public boolean isMissing() {
        return missing;
    }

    public List<ResultLine> getLines() {
        return lines;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public boolean isPayNow() {
        return paymentMode == PaymentMode.PAY_NOW;
    }

    public boolean isPayLater() {
        return paymentMode == PaymentMode.PAY_LATER;
    }

    public String getHeading() {
        if (isPayNow()) {
            return "Checkout complete";
        }
        if (isPayLater()) {
            return "Invoice generated";
        }
        return "Checkout summary";
    }

    public String getIntroText() {
        if (isPayNow()) {
            return "Your order is confirmed. Masked PINs appear below.";
        }
        if (isPayLater()) {
            return "Share the invoice references below with your finance team to complete payment.";
        }
        return "Review the details from your checkout.";
    }

    public Map<String, String> getTotalsByCurrency() {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ResultLine line : lines) {
            if (line.getCurrency() == null) {
                continue;
            }
            totals.merge(line.getCurrency().name(), line.getTotalMinor(), Long::sum);
        }
        Map<String, String> formatted = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            try {
                Price price = new Price(com.theplutushome.veristore.model.Currency.valueOf(entry.getKey()), entry.getValue());
                formatted.put(entry.getKey(), pricingService.format(price));
            } catch (IllegalArgumentException ex) {
                // Ignore unknown currencies
            }
        }
        return formatted;
    }

    public static final class ResultLine implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String reference;
        private final String referenceLabel;
        private final String service;
        private final int quantity;
        private final String totalFormatted;
        private final com.theplutushome.veristore.model.Currency currency;
        private final long totalMinor;
        private final List<String> maskedCodes;
        private final String status;
        private final String checkoutUrl;

        private ResultLine(String reference,
                            String referenceLabel,
                            String service,
                            int quantity,
                            String totalFormatted,
                            com.theplutushome.veristore.model.Currency currency,
                            long totalMinor,
                            List<String> maskedCodes,
                            String status,
                            String checkoutUrl) {
            this.reference = reference;
            this.referenceLabel = referenceLabel;
            this.service = service;
            this.quantity = quantity;
            this.totalFormatted = totalFormatted;
            this.currency = currency;
            this.totalMinor = totalMinor;
            this.maskedCodes = List.copyOf(maskedCodes);
            this.status = status;
            this.checkoutUrl = checkoutUrl;
        }

        static ResultLine fromOrder(OrderStore.Order order, PricingService pricingService) {
            String service = summarizeOrderLines(order.getLines());
            String total = pricingService.format(new Price(order.getCurrency(), order.getTotalMinor()));
            List<String> masked = order.getCodes().stream().map(Masker::mask).toList();
            return new ResultLine(order.getId(),
                    "Order",
                    service,
                    order.getQty(),
                    total,
                    order.getCurrency(),
                    order.getTotalMinor(),
                    masked,
                    null,
                    null);
        }

        static ResultLine fromInvoice(OrderStore.Invoice invoice, PricingService pricingService) {
            String service = summarizeInvoiceLines(invoice.getLines());
            String total = pricingService.format(new Price(invoice.getCurrency(), invoice.getTotalMinor()));
            List<String> masked = invoice.getCodesIfDelivered().stream().map(Masker::mask).toList();
            String status = switch (invoice.getStatus()) {
                case PENDING -> "Pending payment";
                case PAID -> "Paid";
                case CANCELLED -> "Cancelled";
            };
            return new ResultLine(invoice.getInvoiceNo(),
                    "Invoice",
                    service,
                    invoice.getQty(),
                    total,
                    invoice.getCurrency(),
                    invoice.getTotalMinor(),
                    masked,
                    status,
                    invoice.getCheckoutUrl());
        }

        private static String summarizeOrderLines(List<OrderStore.OrderLine> lines) {
            if (lines.isEmpty()) {
                return "";
            }
            return lines.stream()
                    .map(line -> VariantDescriptions.describe(line.getKey().family(), line.getKey().sku())
                            + " x" + line.getQuantity())
                    .collect(Collectors.joining(", "));
        }

        private static String summarizeInvoiceLines(List<OrderStore.InvoiceLine> lines) {
            if (lines.isEmpty()) {
                return "";
            }
            return lines.stream()
                    .map(line -> VariantDescriptions.describe(line.getKey().family(), line.getKey().sku())
                            + " x" + line.getQuantity())
                    .collect(Collectors.joining(", "));
        }

        public String getReference() {
            return reference;
        }

        public String getReferenceLabel() {
            return referenceLabel;
        }

        public String getService() {
            return service;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getTotalFormatted() {
            return totalFormatted;
        }

        public com.theplutushome.veristore.model.Currency getCurrency() {
            return currency;
        }

        public long getTotalMinor() {
            return totalMinor;
        }

        public List<String> getMaskedCodes() {
            return maskedCodes;
        }

        public boolean hasMaskedCodes() {
            return !maskedCodes.isEmpty();
        }

        public String getStatus() {
            return status;
        }

        public boolean hasStatus() {
            return status != null && !status.isBlank();
        }

        public boolean hasCheckoutUrl() {
            return checkoutUrl != null && !checkoutUrl.isBlank();
        }

        public String getCheckoutUrl() {
            return checkoutUrl;
        }
    }
}
