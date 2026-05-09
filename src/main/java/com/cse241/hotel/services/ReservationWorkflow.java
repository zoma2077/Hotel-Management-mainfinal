package com.cse241.hotel.services;

import com.cse241.hotel.enums.ReservationStatus;
import com.cse241.hotel.model.transaction.Reservation;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Single place for reservation state transitions and who may perform them.
 *
 * <p>States: {@code PENDING} (new booking) → {@code CONFIRMED} (staff acknowledged the booking) →
 * {@code CHECKED_IN} (guest on-site / room assigned) → {@code COMPLETED} (paid / checked out), or
 * {@code CANCELLED}.</p>
 */
public final class ReservationWorkflow {

    public enum Actor {
        GUEST,
        STAFF
    }

    private static final Set<ReservationStatus> OCCUPYING =
            EnumSet.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

    private ReservationWorkflow() {
    }

    public static boolean isActiveStay(ReservationStatus status) {
        return status != null && OCCUPYING.contains(status);
    }

    public static boolean staffMayConfirm(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required.");
        return reservation.getStatus() == ReservationStatus.PENDING;
    }

    /**
     * Check-in: staff marks guest as on-site. From {@code CONFIRMED}, or express {@code PENDING} → {@code CHECKED_IN}.
     */
    public static boolean staffMayCheckIn(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required.");
        ReservationStatus s = reservation.getStatus();
        return s == ReservationStatus.CONFIRMED || s == ReservationStatus.PENDING;
    }

    public static boolean staffMayCancel(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required.");
        ReservationStatus s = reservation.getStatus();
        return s == ReservationStatus.PENDING
                || s == ReservationStatus.CONFIRMED
                || s == ReservationStatus.CHECKED_IN;
    }

    public static boolean guestMayCancel(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required.");
        return reservation.getStatus() == ReservationStatus.PENDING;
    }

    /**
     * Checkout screen: settlement after stay or prepayment when booking is firm.
     */
    public static boolean mayOpenCheckout(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required.");
        ReservationStatus s = reservation.getStatus();
        return s == ReservationStatus.CONFIRMED || s == ReservationStatus.CHECKED_IN;
    }

    public static String reasonStaffCannotConfirm(Reservation reservation) {
        if (staffMayConfirm(reservation)) {
            return null;
        }
        return "Only PENDING reservations can be confirmed (current: " + reservation.getStatus() + ").";
    }

    public static String reasonStaffCannotCheckIn(Reservation reservation) {
        if (staffMayCheckIn(reservation)) {
            return null;
        }
        return "Check-in requires PENDING or CONFIRMED status (current: " + reservation.getStatus() + ").";
    }

    public static String reasonStaffCannotCancel(Reservation reservation) {
        if (staffMayCancel(reservation)) {
            return null;
        }
        return "Cannot cancel a reservation that is " + reservation.getStatus() + ".";
    }

    public static String reasonGuestCannotCancel(Reservation reservation) {
        if (guestMayCancel(reservation)) {
            return null;
        }
        return "Guests may cancel only while the booking is still PENDING.";
    }

    public static String reasonCheckoutBlocked(Reservation reservation) {
        if (mayOpenCheckout(reservation)) {
            return null;
        }
        return "Checkout is available only for CONFIRMED or CHECKED_IN reservations.";
    }
}
