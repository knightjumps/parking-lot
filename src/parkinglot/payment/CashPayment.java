package parkinglot.payment;

import java.math.BigDecimal;
import java.util.UUID;

public final class CashPayment implements Payment {
    public PaymentReceipt pay(String ticketId, BigDecimal amount) {
        return new PaymentReceipt(true, "CASH-" + UUID.randomUUID().toString().substring(0, 8), "Cash accepted");
    }
}
