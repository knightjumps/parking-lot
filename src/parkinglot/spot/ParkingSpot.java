package parkinglot.spot;

import parkinglot.vehicle.Vehicle;

/**
 * A spot enforces compatibility and is the only owner of its occupancy state.
 */
public abstract class ParkingSpot {
    private final String spotId;
    private Vehicle parkedVehicle;

    protected ParkingSpot(String spotId) {
        this.spotId = spotId;
    }

    public String spotId() {
        return spotId;
    }

    public abstract SpotType type();

    protected abstract boolean canFit(Vehicle vehicle);

    public final boolean tryAssign(Vehicle vehicle) {
        if (parkedVehicle != null || !canFit(vehicle)) return false;
        parkedVehicle = vehicle;
        return true;
    }

    /**
     * @return true only if a vehicle was actually released.
     */
    public final boolean release() {
        if (parkedVehicle == null) return false;
        parkedVehicle = null;
        return true;
    }
}
