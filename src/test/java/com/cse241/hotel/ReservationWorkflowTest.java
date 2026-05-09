package com.cse241.hotel;

import com.cse241.hotel.db.HotelDatabase;
import com.cse241.hotel.enums.ReservationStatus;
import com.cse241.hotel.model.transaction.Reservation;
import com.cse241.hotel.services.ReservationWorkflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationWorkflowTest {

    @BeforeEach
    void setUp() {
        HotelDatabase.resetForTests();
    }

    @Test
    void staffConfirmOnlyFromPending() {
        Reservation pending = findFirstWithStatus(ReservationStatus.PENDING);
        Reservation confirmed = findFirstWithStatus(ReservationStatus.CONFIRMED);

        assertTrue(ReservationWorkflow.staffMayConfirm(pending));
        assertNotNull(ReservationWorkflow.reasonStaffCannotConfirm(confirmed));
        assertFalse(ReservationWorkflow.staffMayConfirm(confirmed));
    }

    @Test
    void staffCheckInFromPendingOrConfirmed() {
        Reservation pending = findFirstWithStatus(ReservationStatus.PENDING);
        Reservation confirmed = findFirstWithStatus(ReservationStatus.CONFIRMED);

        assertTrue(ReservationWorkflow.staffMayCheckIn(pending));
        assertTrue(ReservationWorkflow.staffMayCheckIn(confirmed));
    }

    @Test
    void guestMayCancelOnlyPending() {
        Reservation pending = findFirstWithStatus(ReservationStatus.PENDING);
        Reservation confirmed = findFirstWithStatus(ReservationStatus.CONFIRMED);

        assertTrue(ReservationWorkflow.guestMayCancel(pending));
        assertFalse(ReservationWorkflow.guestMayCancel(confirmed));
    }

    @Test
    void checkoutOnlyConfirmedOrCheckedIn() {
        Reservation pending = findFirstWithStatus(ReservationStatus.PENDING);
        Reservation confirmed = findFirstWithStatus(ReservationStatus.CONFIRMED);

        assertFalse(ReservationWorkflow.mayOpenCheckout(pending));
        assertTrue(ReservationWorkflow.mayOpenCheckout(confirmed));
    }

    @Test
    void checkoutAllowedWhenCheckedIn() {
        Reservation confirmed = HotelDatabase.RESERVATIONS.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .findFirst()
                .orElseThrow();
        confirmed.setStatus(ReservationStatus.CHECKED_IN);
        assertTrue(ReservationWorkflow.mayOpenCheckout(confirmed));
    }

    private static Reservation findFirstWithStatus(ReservationStatus status) {
        return HotelDatabase.RESERVATIONS.stream()
                .filter(r -> r.getStatus() == status)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No reservation with status " + status));
    }
}
