package parkinglot.vehicle;

/**
 * Base type keeps common data; child classes define the vehicle category.
 */
public abstract class Vehicle {
    private final String licenseNumber;

    protected Vehicle(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String licenseNumber() {
        return licenseNumber;
    }

    public abstract VehicleType type();

    @Override
    public String toString() {
        return type() + "(" + licenseNumber + ")";
    }
}
