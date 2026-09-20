package parkinglot.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Example extension: validate coupons here or delegate validation to a promotion service.
 */
public final class CouponPayment implements Payment {
    private final String couponCode;

    public CouponPayment(String couponCode) {
        this.couponCode = couponCode;
    }

    public PaymentReceipt pay(String ticketId, BigDecimal amount) {
        if (couponCode == null || couponCode.isBlank()) return new PaymentReceipt(false, null, "Missing coupon code");
        return new PaymentReceipt(true, "COUPON-" + UUID.randomUUID().toString().substring(0, 8), "Coupon accepted");
    }
}
