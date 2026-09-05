package parkinglot.spot;
import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleType;
public final class MotorcycleSpot extends ParkingSpot {
    public MotorcycleSpot(String id) { super(id); }
    public SpotType type() { return SpotType.MOTORCYCLE; }
    protected boolean canFit(Vehicle vehicle) { return vehicle.type() == VehicleType.MOTORCYCLE; }
}
