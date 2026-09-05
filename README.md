# Parking Lot LLD (Java)

Run `Main.java` to exercise the complete happy path: configure a floor and spots, issue tickets, show availability, accept card/cash payments, and release spots at exit.

```text
ParkingLot (orchestrates)
  -> ParkingFloor (owns spots and availability counts)
      -> ParkingSpot subclasses (fit rules + occupancy)
  -> ParkingTicket (one parking session)
  -> PricingStrategy (fee calculation)
  -> Payment (cash/card collection)
  -> EntrancePanel / ExitPanel / ParkingAttendant (entry and checkout channels)
```

## Why the boundaries matter

- `ParkingSpot` owns its occupancy, so no other class can accidentally make a spot inconsistent.
- `ParkingFloor` updates its counters as a spot changes, avoiding expensive full scans to render a display board.
- `PricingStrategy` makes flat, event, dynamic, or vehicle-specific prices additive changes.
- `Payment` lets checkout use cash, cards, coupons, or a future payment gateway without rewriting its workflow.
- `ParkingLot.park()` and `checkout()` are synchronized: in this one-JVM demo, two entrance panels cannot reserve the same final spot.

## Production extensions

- Add `EntrancePanel`, `ExitPanel`, `ParkingAttendant`, and `Admin` as adapters over the current service methods.
- Store tickets and spots in a database for restarts and audit history.
- For multiple backend instances, replace JVM synchronization with database transactions plus row/optimistic locking.
- Model accessible-space eligibility through a dedicated policy; the sample does not guess at accessibility eligibility.
