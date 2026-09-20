package parkinglot;

import parkinglot.spot.ParkingSpot;
import parkinglot.spot.SpotType;
import parkinglot.vehicle.Vehicle;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Owns a floor's spots and keeps availability counters consistent with their state.
 */
final class ParkingFloor {
    private final String floorId;
    private final Map<String, ParkingSpot> spots = new LinkedHashMap<>();
    private final Map<SpotType, Integer> availableByType = new EnumMap<>(SpotType.class);

    ParkingFloor(String floorId) {
        this.floorId = floorId;
        for (SpotType type : SpotType.values()) availableByType.put(type, 0);
    }

    String floorId() {
        return floorId;
    }

    void addSpot(ParkingSpot spot) {
        if (spots.putIfAbsent(spot.spotId(), spot) != null)
            throw new IllegalArgumentException("Spot already exists: " + spot.spotId());
        availableByType.merge(spot.type(), 1, Integer::sum);
    }

    ParkingSpot reserveSpotFor(Vehicle vehicle) {
        for (ParkingSpot spot : spots.values()) {
            if (spot.tryAssign(vehicle)) {
                availableByType.merge(spot.type(), -1, Integer::sum);
                return spot;
            }
        }
        return null;
    }

    void releaseSpot(String spotId) {
        ParkingSpot spot = spots.get(spotId);
        if (spot == null) throw new NoSuchElementException("Unknown spot: " + spotId);
        if (spot.release()) availableByType.merge(spot.type(), 1, Integer::sum);
    }

    String displayBoard() {
        return "%s -> compact=%d, large=%d, motorcycle=%d, accessible=%d".formatted(floorId,
                availableByType.get(SpotType.COMPACT), availableByType.get(SpotType.LARGE),
                availableByType.get(SpotType.MOTORCYCLE), availableByType.get(SpotType.ACCESSIBLE));
    }
}
