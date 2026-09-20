package parkinglot.pricing;

import parkinglot.ParkingTicket;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Strategy pattern: checkout does not depend on one particular fee algorithm.
 */
public interface PricingStrategy {
    BigDecimal calculate(ParkingTicket ticket, Instant exitTime);
}
