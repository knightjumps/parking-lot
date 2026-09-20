package parkinglot.vehicle;

public final class Car extends Vehicle {
    public Car(String plate) {
        super(plate);
    }

    public VehicleType type() {
        return VehicleType.CAR;
    }
}
