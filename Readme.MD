# Enrollment checkout flows

The storefront exposes four guided enrollment purchase tracks. Each flow below follows the journey from selecting the track on the home page to receiving PIN delivery, with parallel paths for paying immediately or via an invoice.

## First issuance

**Selection & package setup**
1. Choose the **First issuance** card on the index page to launch the wizard for that track.【F:src/main/webapp/index.xhtml†L16-L59】
2. Progress through the wizard: pick the applicant's citizenship, then choose a package and quantity before adding it to the cart.【F:src/main/java/com/theplutushome/veristore/controller/PurchaseView.java†L168-L237】【F:src/main/webapp/buy-first-issuance.xhtml†L90-L195】
3. Open the cart and proceed to checkout once the desired packages are in the basket.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L173-L276】

**Pay now path**
1. Keep **Pay now** selected on the checkout screen, confirm email and phone delivery details, and review the order summary.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Submitting the order invokes `payNow`, which issues an invoice and (when available) redirects to the payment gateway to complete the transaction immediately.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L67-L114】
3. After the payment callback, the system redeems the invoice, pulls the required PINs, and emails them to the contact (SMS delivery is logged but not yet implemented).【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L117-L267】

**Pay later path**
1. Switch the payment option to **Pay later** while keeping the same delivery details on the checkout page.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Submitting in this mode triggers `payLater`, generating an invoice without a checkout redirect so the buyer can settle it offline.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L75-L185】
3. When finance marks the invoice as paid (via manual redemption or gateway callback), fulfillment runs and PINs are emailed to the supplied address.【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L83-L267】

## Renewal

**Selection & package setup**
1. Launch the renewal wizard from the **Renewal** card on the index page.【F:src/main/webapp/index.xhtml†L16-L59】
2. Select the applicant citizenship and then the renewal package duration/quantity to add to the cart.【F:src/main/java/com/theplutushome/veristore/controller/PurchaseView.java†L168-L237】【F:src/main/webapp/buy-renewal.xhtml†L90-L198】
3. Review the cart and advance to checkout when ready.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L173-L276】

**Pay now path**
1. Leave **Pay now** active, provide the required contact channels, and review totals on checkout.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Submission calls `payNow`, producing an invoice and optional payment redirect for immediate settlement.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L67-L185】
3. Successful payment fulfillment retrieves renewal PINs and emails them to the configured contact.【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L117-L267】

**Pay later path**
1. Choose **Pay later** on checkout, keeping the same contact preferences.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Submitting generates a pay-later invoice for offline payment without redirecting to a gateway.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L75-L185】
3. Once the invoice is paid and redeemed, the fulfillment pipeline emails the renewal PINs (with SMS noted as pending implementation).【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L83-L267】

## Replacement

**Selection & package setup**
1. Begin from the **Replacement** card on the storefront to open the replacement wizard.【F:src/main/webapp/index.xhtml†L16-L59】
2. Choose the applicant citizenship, select the replacement package, set the quantity, and add it to the cart.【F:src/main/java/com/theplutushome/veristore/controller/PurchaseView.java†L168-L237】【F:src/main/webapp/buy-replacement.xhtml†L89-L199】
3. Use the cart to proceed to checkout for payment configuration.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L173-L276】

**Pay now path**
1. With **Pay now** selected, confirm the delivery email and phone and review the summary at checkout.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Completing checkout invokes the immediate payment invoice flow and optional redirect for gateway collection.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L67-L185】
3. After payment confirmation, the system fulfills the invoice and emails the replacement PINs to the buyer.【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L117-L267】

**Pay later path**
1. Switch to **Pay later** before submitting checkout so the buyer receives an invoice instead of a redirect.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. The pay-later submission creates an invoice that can be settled offline and revisited for redemption.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L75-L185】
3. When the invoice status becomes paid, fulfillment distributes the replacement PINs by email (SMS noted as pending).【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L83-L267】

## Update

**Selection & package setup**
1. Enter the update flow via the **Update** card on the home page.【F:src/main/webapp/index.xhtml†L16-L59】
2. Work through the wizard: select citizenship, choose a service tier when the applicant is a citizen, then pick the appropriate update package and quantity before adding it to the cart.【F:src/main/java/com/theplutushome/veristore/controller/PurchaseView.java†L168-L237】【F:src/main/java/com/theplutushome/veristore/controller/PurchaseView.java†L427-L604】【F:src/main/webapp/buy-update.xhtml†L78-L212】
3. Continue to checkout from the cart once every required SKU has been captured.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L173-L276】

**Pay now path**
1. Keep **Pay now** active on checkout, confirm the delivery contacts, and review the order summary.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. Submitting engages the immediate payment flow, issuing an invoice and redirecting when a hosted checkout URL is available.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L67-L185】
3. Payment completion triggers fulfillment to fetch the update PINs and send them via email to the stored contact.【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L117-L267】

**Pay later path**
1. Change the payment selection to **Pay later** prior to submission, leaving the contact details intact.【F:src/main/webapp/checkout.xhtml†L50-L158】【F:src/main/java/com/theplutushome/veristore/controller/CheckoutView.java†L48-L217】
2. The checkout call produces a deferred invoice that teams can pay after internal approvals.【F:src/main/java/com/theplutushome/veristore/controller/CartView.java†L222-L275】【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L75-L185】
3. When the invoice is redeemed, fulfillment retrieves the update PINs and sends them to the configured delivery channels (email active, SMS pending implementation).【F:src/main/java/com/theplutushome/veristore/service/payment/PaymentService.java†L83-L267】
