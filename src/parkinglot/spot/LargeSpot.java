package parkinglot.spot;
import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleType;
public final class LargeSpot extends ParkingSpot {
    public LargeSpot(String id) { super(id); }
    public SpotType type() { return SpotType.LARGE; }
    protected boolean canFit(Vehicle vehicle) { return vehicle.type() == VehicleType.CAR || vehicle.type() == VehicleType.TRUCK || vehicle.type() == VehicleType.VAN; }
}
