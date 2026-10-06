package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

  @Mock
  private PaymentService paymentService;

  @Mock
  private SeatReservationService seatReservationService;

  private CinemaTicketsService cinemaTicketsService;

  @BeforeEach
  void setUp() {
    cinemaTicketsService = new CinemaTicketsServiceImpl(paymentService, seatReservationService);
  }

  @Test
  void givenAdultChildAndInfantTickets_whenPurchaseTickets_thenPaymentTakenAndSeatsReserved() {
    final BookingConfirmation bookingConfirmation = cinemaTicketsService.purchaseTickets(123L,
        new TicketRequest(ADULT, 2),
        new TicketRequest(CHILD, 3),
        new TicketRequest(INFANT, 1));

    assertEquals(123L, bookingConfirmation.accountId());
    verify(paymentService).debitAccount(123L, BigDecimal.valueOf(104.48));
    verify(seatReservationService).reserveSeats(123L, 5L);
  }

  @Test
  void givenMultipleRequestsForSameTicketType_whenPurchaseTickets_thenTicketsAreCombined() {
    cinemaTicketsService.purchaseTickets(123L,
        new TicketRequest(ADULT, 1),
        new TicketRequest(CHILD, 1),
        new TicketRequest(CHILD, 2));

    verify(paymentService).debitAccount(123L, BigDecimal.valueOf(78.49));
    verify(seatReservationService).reserveSeats(123L, 4L);
  }

  @Test
  void givenOnlyInfantTickets_whenPurchaseTickets_thenBookingIsRejected() {
    assertThrows(InvalidBookingException.class,
        () -> cinemaTicketsService.purchaseTickets(123L, new TicketRequest(INFANT, 1)));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenOnlyChildTickets_whenPurchaseTickets_thenBookingIsRejected() {
    assertThrows(InvalidBookingException.class,
        () -> cinemaTicketsService.purchaseTickets(123L, new TicketRequest(CHILD, 1)));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenMoreThanTwentyFiveTickets_whenPurchaseTickets_thenBookingIsRejected() {
    assertThrows(InvalidBookingException.class,
        () -> cinemaTicketsService.purchaseTickets(123L,
            new TicketRequest(ADULT, 1),
            new TicketRequest(CHILD, 25)));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenInvalidAccountId_whenPurchaseTickets_thenBookingIsRejected() {
    assertThrows(InvalidBookingException.class,
        () -> cinemaTicketsService.purchaseTickets(0L, new TicketRequest(ADULT, 1)));

    verifyNoInteractions(paymentService, seatReservationService);
  }

  @Test
  void givenInvalidTicketCount_whenPurchaseTickets_thenBookingIsRejected() {
    assertThrows(InvalidBookingException.class,
        () -> cinemaTicketsService.purchaseTickets(123L, new TicketRequest(ADULT, 0)));

    verifyNoInteractions(paymentService, seatReservationService);
  }
}
