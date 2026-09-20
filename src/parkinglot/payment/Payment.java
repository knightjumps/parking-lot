package parkinglot.payment;

import java.math.BigDecimal;

/**
 * Hides cash/card/coupon mechanics from the checkout use case.
 */
public interface Payment {
    PaymentReceipt pay(String ticketId, BigDecimal amount);
}
