package parkinglot.payment;
public record PaymentReceipt(boolean successful, String paymentReference, String message) { }
