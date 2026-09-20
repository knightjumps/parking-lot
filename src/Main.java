import parkinglot.ParkingLot;
import parkinglot.EntrancePanel;
import parkinglot.ExitPanel;
import parkinglot.payment.CashPayment;
import parkinglot.payment.CardPayment;
import parkinglot.spot.CompactSpot;
import parkinglot.spot.LargeSpot;
import parkinglot.spot.MotorcycleSpot;
import parkinglot.vehicle.Car;
import parkinglot.vehicle.Motorcycle;
import parkinglot.vehicle.Truck;

/**
 * Small executable example. Run this class from IntelliJ to see the main parking flow.
 */
public class Main {
    public static void main(String[] args) {
        ParkingLot lot = new ParkingLot("City Center Parking");
        lot.addFloor("F1");
        lot.addSpot("F1", new MotorcycleSpot("M-01"));
        lot.addSpot("F1", new CompactSpot("C-01"));
        lot.addSpot("F1", new LargeSpot("L-01"));
        EntrancePanel entrance = new EntrancePanel("ENTRY-1", lot);
        ExitPanel exit = new ExitPanel("EXIT-1", lot);

        System.out.println(lot.availability());
        var carTicket = entrance.issueTicket(new Car("KA-01-AB-1234"));
        var truckTicket = entrance.issueTicket(new Truck("KA-02-CD-5678"));
        var bikeTicket = entrance.issueTicket(new Motorcycle("KA-03-EF-9012"));
        System.out.println("Car ticket: " + carTicket);
        System.out.println("Truck ticket: " + truckTicket);
        System.out.println("Bike ticket: " + bikeTicket);
        System.out.println(lot.availability());

        System.out.println(exit.acceptPayment(carTicket.ticketId(), new CardPayment("tok_demo_4242")));
        System.out.println(exit.acceptPayment(truckTicket.ticketId(), new CashPayment()));
        System.out.println(exit.acceptPayment(bikeTicket.ticketId(), new CashPayment()));
        System.out.println(lot.availability());
    }
}
