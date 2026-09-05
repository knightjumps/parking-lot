package parkinglot.spot;
import parkinglot.vehicle.Vehicle;
/** Accessibility eligibility belongs in a dedicated customer/vehicle policy, not an assumption. */
public final class AccessibleSpot extends ParkingSpot {
    public AccessibleSpot(String id) { super(id); }
    public SpotType type() { return SpotType.ACCESSIBLE; }
    protected boolean canFit(Vehicle vehicle) { return false; }
}
