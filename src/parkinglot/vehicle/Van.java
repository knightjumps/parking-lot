package parkinglot.vehicle;

public final class Van extends Vehicle {
    public Van(String plate) {
        super(plate);
    }

    public VehicleType type() {
        return VehicleType.VAN;
    }
}
