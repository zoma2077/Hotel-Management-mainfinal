package com.cse241.hotel;

import com.cse241.hotel.db.HotelDatabase;
import com.cse241.hotel.enums.PaymentMethod;
import com.cse241.hotel.enums.ReservationStatus;
import com.cse241.hotel.exceptions.InvalidPaymentException;
import com.cse241.hotel.model.transaction.Invoice;
import com.cse241.hotel.model.transaction.Reservation;
import com.cse241.hotel.model.user.Guest;
import com.cse241.hotel.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    @BeforeEach
    void setUp() {
        HotelDatabase.resetForTests();
    }

    @Test
    void onlinePaymentShouldThrowWhenBalanceInsufficient() {
        Reservation reservation = HotelDatabase.RESERVATIONS.get(0);
        Guest guest = reservation.getGuest();
        guest.debitBalance(guest.getBalance()); // set to zero

        Invoice invoice = new Invoice(reservation, new BigDecimal("10.00"));

        assertThrows(InvalidPaymentException.class, () -> PaymentService.processPayment(invoice, PaymentMethod.ONLINE));
    }

    @Test
    void payingAlreadyPaidInvoiceShouldThrow() {
        Reservation reservation = HotelDatabase.RESERVATIONS.get(0);
        Invoice invoice = new Invoice(reservation, new BigDecimal("10.00"));
        invoice.markPaid(PaymentMethod.CASH, LocalDateTime.now());

        assertThrows(InvalidPaymentException.class, () -> PaymentService.processPayment(invoice, PaymentMethod.CASH));
    }

    @Test
    void checkoutShouldRejectPendingReservation() {
        Reservation pending = HotelDatabase.RESERVATIONS.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING)
                .findFirst()
                .orElseThrow();

        assertThrows(InvalidPaymentException.class,
                () -> PaymentService.checkout(pending, PaymentMethod.CASH));
    }

    @Test
    void checkoutWithCashShouldDebitGuestBalanceAndCompleteReservation() throws InvalidPaymentException {
        Reservation reservation = HotelDatabase.RESERVATIONS.get(0);
        Guest guest = reservation.getGuest();
        BigDecimal before = guest.getBalance();
        BigDecimal total = PaymentService.calculateTotal(reservation);

        PaymentService.checkout(reservation, PaymentMethod.CASH);

        assertEquals(0, before.subtract(total).compareTo(guest.getBalance()));
        assertEquals(ReservationStatus.COMPLETED, reservation.getStatus());
    }

    @Test
    void checkoutWithCreditCardShouldDebitGuestBalance() throws InvalidPaymentException {
        Reservation reservation = HotelDatabase.RESERVATIONS.get(0);
        Guest guest = reservation.getGuest();
        BigDecimal before = guest.getBalance();
        BigDecimal total = PaymentService.calculateTotal(reservation);

        PaymentService.checkout(reservation, PaymentMethod.CREDIT_CARD);

        assertEquals(0, before.subtract(total).compareTo(guest.getBalance()));
    }
}

