package parkinglot.pricing;

import parkinglot.ParkingTicket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * First hour $4, hours 2-3 $3.50 each, all later hours $2.50 each.
 */
public final class HourlyPricingStrategy implements PricingStrategy {
    public BigDecimal calculate(ParkingTicket ticket, Instant exitTime) {
        long minutes = Math.max(1, Duration.between(ticket.entryTime(), exitTime).toMinutes());
        long hours = Math.max(1, (long) Math.ceil(minutes / 60.0));
        BigDecimal amount = new BigDecimal("4.00");
        if (hours > 1) amount = amount.add(new BigDecimal("3.50").multiply(BigDecimal.valueOf(Math.min(hours - 1, 2))));
        if (hours > 3) amount = amount.add(new BigDecimal("2.50").multiply(BigDecimal.valueOf(hours - 3)));
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
