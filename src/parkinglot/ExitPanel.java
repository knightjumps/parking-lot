package parkinglot;

import parkinglot.payment.Payment;

/** Hardware/API adapter for an exit gate; it only opens when checkout authorizes exit. */
public final class ExitPanel {
    private final String panelId;
    private final ParkingLot parkingLot;
    public ExitPanel(String panelId, ParkingLot parkingLot) { this.panelId = panelId; this.parkingLot = parkingLot; }
    public ExitReceipt acceptPayment(String ticketId, Payment payment) { return parkingLot.checkout(ticketId, payment); }
    public String panelId() { return panelId; }
}
