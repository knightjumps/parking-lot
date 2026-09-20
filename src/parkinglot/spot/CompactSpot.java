package parkinglot.spot;

import parkinglot.vehicle.Vehicle;
import parkinglot.vehicle.VehicleType;

public final class CompactSpot extends ParkingSpot {
    public CompactSpot(String id) {
        super(id);
    }

    public SpotType type() {
        return SpotType.COMPACT;
    }

    protected boolean canFit(Vehicle vehicle) {
        return vehicle.type() == VehicleType.CAR;
    }
}
