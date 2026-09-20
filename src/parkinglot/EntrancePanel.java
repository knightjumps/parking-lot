package parkinglot;

import parkinglot.vehicle.Vehicle;

/**
 * Hardware/API adapter for an entry gate. The business rule remains in ParkingLot.
 */
public final class EntrancePanel {
    private final String panelId;
    private final ParkingLot parkingLot;

    public EntrancePanel(String panelId, ParkingLot parkingLot) {
        this.panelId = panelId;
        this.parkingLot = parkingLot;
    }

    public ParkingTicket issueTicket(Vehicle vehicle) {
        return parkingLot.park(vehicle);
    }

    public String panelId() {
        return panelId;
    }
}
