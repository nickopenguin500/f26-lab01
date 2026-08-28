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
        BookingResult.Confirmed r1 = (BookingResult.Confirmed) svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(600, 660)); // Bob gets waitlisted
        
        svc.cancelBooking(r1.booking().id());
        
        java.util.List<edu.cmu.cs214.booking.domain.Booking> bookings = svc.listBookings(roomA);
        assertEquals(1, bookings.size());
        assertEquals(bob, bookings.get(0).user());
        assertEquals(0, store.waitlistForRoom(roomA).size());
    }

    @Test
    void isAvailableWhenBookingEndsDuringInterval() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        boolean available = svc.isAvailable(roomA, new TimeInterval(630, 700));
        assertEquals(false, available);
    }
}
