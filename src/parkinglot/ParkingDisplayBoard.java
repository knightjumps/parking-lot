package parkinglot;

/** Read model for a physical display. A real board would poll or consume availability events. */
public final class ParkingDisplayBoard {
    private final ParkingLot parkingLot;
    public ParkingDisplayBoard(ParkingLot parkingLot) { this.parkingLot = parkingLot; }
    public String render() { return parkingLot.availability(); }
}
