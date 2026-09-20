package parkinglot;

import parkinglot.payment.Payment;

/**
 * A human-assisted checkout channel; it shares exactly the same exit workflow as a panel.
 */
public final class ParkingAttendant {
    private final String employeeId;
    private final ParkingLot parkingLot;

    public ParkingAttendant(String employeeId, ParkingLot parkingLot) {
        this.employeeId = employeeId;
        this.parkingLot = parkingLot;
    }

    public ExitReceipt processPayment(String ticketId, Payment payment) {
        return parkingLot.checkout(ticketId, payment);
    }

    public String employeeId() {
        return employeeId;
    }
}
