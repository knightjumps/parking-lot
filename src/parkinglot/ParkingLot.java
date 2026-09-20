package parkinglot;

import parkinglot.payment.Payment;
import parkinglot.payment.PaymentReceipt;
import parkinglot.pricing.HourlyPricingStrategy;
import parkinglot.pricing.PricingStrategy;
import parkinglot.spot.ParkingSpot;
import parkinglot.vehicle.Vehicle;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Coordinates parking, payment, and exit; it delegates spot and pricing rules.
 */
public final class ParkingLot {
    private final String name;
    private final Map<String, ParkingFloor> floors = new LinkedHashMap<>();
    private final Map<String, ParkingTicket> activeTickets = new LinkedHashMap<>();
    private final PricingStrategy pricingStrategy;
    private final Clock clock;

    public ParkingLot(String name) {
        this(name, new HourlyPricingStrategy(), Clock.systemUTC());
    }

    public ParkingLot(String name, PricingStrategy pricingStrategy, Clock clock) {
        this.name = name;
        this.pricingStrategy = pricingStrategy;
        this.clock = clock;
    }

    public void addFloor(String floorId) {
        if (floors.putIfAbsent(floorId, new ParkingFloor(floorId)) != null) {
            throw new IllegalArgumentException("Floor already exists: " + floorId);
        }
    }

    public void addSpot(String floorId, ParkingSpot spot) {
        floor(floorId).addSpot(spot);
    }

    /**
     * Single-JVM atomic reservation: two entrances cannot receive the same final spot.
     */
    public synchronized ParkingTicket park(Vehicle vehicle) {
        for (ParkingFloor floor : floors.values()) {
            ParkingSpot spot = floor.reserveSpotFor(vehicle);
            if (spot != null) {
                String ticketId = "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                ParkingTicket ticket = new ParkingTicket(ticketId, vehicle, floor.floorId(), spot.spotId(), Instant.now(clock));
                activeTickets.put(ticketId, ticket);
                return ticket;
            }
        }
        throw new ParkingUnavailableException("No compatible free spot for " + vehicle.type());
    }

    /**
     * Successful payment authorizes exit and releases only the ticket's assigned spot.
     */
    public synchronized ExitReceipt checkout(String ticketId, Payment payment) {
        ParkingTicket ticket = ticket(ticketId);
        Instant exitTime = Instant.now(clock);
        BigDecimal amount = pricingStrategy.calculate(ticket, exitTime);
        PaymentReceipt receipt = payment.pay(ticket.ticketId(), amount);
        if (!receipt.successful()) return ExitReceipt.paymentFailed(ticketId, amount, receipt.message());
        floor(ticket.floorId()).releaseSpot(ticket.spotId());
        ticket.markPaid(exitTime, amount, receipt.paymentReference());
        activeTickets.remove(ticketId);
        return ExitReceipt.success(ticketId, amount, receipt.paymentReference());
    }

    public synchronized String availability() {
        List<String> lines = new ArrayList<>(List.of("Availability for " + name + ":"));
        floors.values().forEach(floor -> lines.add("  " + floor.displayBoard()));
        return String.join(System.lineSeparator(), lines);
    }

    private ParkingFloor floor(String floorId) {
        ParkingFloor floor = floors.get(floorId);
        if (floor == null) throw new NoSuchElementException("Unknown floor: " + floorId);
        return floor;
    }

    private ParkingTicket ticket(String ticketId) {
        ParkingTicket ticket = activeTickets.get(ticketId);
        if (ticket == null) throw new NoSuchElementException("Active ticket not found: " + ticketId);
        return ticket;
    }
}
