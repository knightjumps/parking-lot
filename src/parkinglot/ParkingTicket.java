package parkinglot;

import parkinglot.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Identity and payment state for exactly one vehicle visit.
 */
public final class ParkingTicket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final String floorId;
    private final String spotId;
    private final Instant entryTime;
    private TicketStatus status = TicketStatus.ACTIVE;
    private Instant exitTime;
    private BigDecimal paidAmount;
    private String paymentReference;

    ParkingTicket(String ticketId, Vehicle vehicle, String floorId, String spotId, Instant entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.floorId = floorId;
        this.spotId = spotId;
        this.entryTime = entryTime;
    }

    public String ticketId() {
        return ticketId;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public String floorId() {
        return floorId;
    }

    public String spotId() {
        return spotId;
    }

    public Instant entryTime() {
        return entryTime;
    }

    public TicketStatus status() {
        return status;
    }

    void markPaid(Instant exitTime, BigDecimal amount, String reference) {
        this.exitTime = exitTime;
        this.paidAmount = amount;
        this.paymentReference = reference;
        this.status = TicketStatus.PAID;
    }

    @Override
    public String toString() {
        return "%s [%s at %s/%s]".formatted(ticketId, vehicle, floorId, spotId);
    }
}
