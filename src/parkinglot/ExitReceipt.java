package parkinglot;
import java.math.BigDecimal;
/** Result returned to an exit panel after payment is attempted. */
public record ExitReceipt(String ticketId, boolean exitAuthorized, BigDecimal amount, String detail) {
    static ExitReceipt success(String id, BigDecimal amount, String reference) { return new ExitReceipt(id, true, amount, "Paid: " + reference); }
    static ExitReceipt paymentFailed(String id, BigDecimal amount, String reason) { return new ExitReceipt(id, false, amount, "Payment failed: " + reason); }
}
