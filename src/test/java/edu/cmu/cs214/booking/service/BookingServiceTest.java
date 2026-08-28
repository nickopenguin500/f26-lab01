package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class BookingServiceTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final Room roomB = new Room("B", "Beta", 4);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");

    private BookingService newService() {
        return new BookingService(new InMemoryBookingStore());
    }

    @Test
    void bookConfirmsWhenRoomIsFree() {
        BookingService svc = newService();
        BookingResult r = svc.book(roomA, alice, new TimeInterval(600, 660));
        assertInstanceOf(BookingResult.Confirmed.class, r);
    }

    @Test
    void bookWaitlistsWhenSlotIsTaken() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        BookingResult r = svc.book(roomA, bob, new TimeInterval(630, 700));
        assertInstanceOf(BookingResult.Waitlisted.class, r);
    }

    @Test
    void backToBackBookingsAreConfirmed() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        BookingResult r = svc.book(roomA, bob, new TimeInterval(660, 720));
        assertInstanceOf(BookingResult.Confirmed.class, r);
    }

    @Test
    void sameSlotInDifferentRoomsAreBothConfirmed() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        BookingResult r = svc.book(roomB, bob, new TimeInterval(600, 660));
        assertInstanceOf(BookingResult.Confirmed.class, r);
    }

    @Test
    void listBookingsReturnsConfirmedBookings() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(660, 720));
        assertEquals(2, svc.listBookings(roomA).size());
    }

    @Test
    void cancelBookingPromotesWaitlistedUser() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);
        
        User charlie = new User("u3", "Charlie");
        User dave = new User("u4", "Dave");

        // Confirmed bookings
        BookingResult.Confirmed r1 = (BookingResult.Confirmed) svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, charlie, new TimeInterval(660, 720));
        
        // Waitlisted users
        svc.book(roomA, bob, new TimeInterval(600, 720)); // First in line, overlaps with Alice AND Charlie
        svc.book(roomA, dave, new TimeInterval(600, 660)); // Second in line, overlaps only with Alice
        
        // Cancel Alice's booking
        svc.cancelBooking(r1.booking().id());
        
        // Bob still overlaps with Charlie, so Dave should be promoted instead.
        java.util.List<edu.cmu.cs214.booking.domain.Booking> bookings = svc.listBookings(roomA);
        assertEquals(2, bookings.size()); // Charlie and Dave
        
        boolean hasCharlie = bookings.stream().anyMatch(b -> b.user().equals(charlie));
        boolean hasDave = bookings.stream().anyMatch(b -> b.user().equals(dave));
        assertEquals(true, hasCharlie);
        assertEquals(true, hasDave);
        
        // Waitlist should still contain Bob
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals(bob, store.waitlistForRoom(roomA).get(0).user());
    }

    @Test
    void isAvailableWhenBookingEndsDuringInterval() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        boolean available = svc.isAvailable(roomA, new TimeInterval(630, 700));
        assertEquals(false, available);
    }
}
