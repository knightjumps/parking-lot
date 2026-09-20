package parkinglot.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Production code would call a payment gateway; this class simulates authorization.
 */
public final class CardPayment implements Payment {
    private final String paymentToken;

    public CardPayment(String paymentToken) {
        this.paymentToken = paymentToken;
    }

    public PaymentReceipt pay(String ticketId, BigDecimal amount) {
        if (paymentToken == null || paymentToken.isBlank())
            return new PaymentReceipt(false, null, "Missing card token");
        return new PaymentReceipt(true, "CARD-" + UUID.randomUUID().toString().substring(0, 8), "Card approved");
    }
}
