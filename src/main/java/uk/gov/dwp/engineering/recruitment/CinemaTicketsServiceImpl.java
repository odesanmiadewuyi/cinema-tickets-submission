package uk.gov.dwp.engineering.recruitment;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private static final int MAXIMUM_TICKETS_PER_PURCHASE = 25;

  private static final BigDecimal CHILD_TICKET_PRICE = BigDecimal.valueOf(17.50);

  private static final BigDecimal ADULT_TICKET_PRICE = BigDecimal.valueOf(25.99);

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  public CinemaTicketsServiceImpl(PaymentService paymentService,
      SeatReservationService seatReservationService) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    validateAccountId(accountId);

    final Map<TicketType, Integer> ticketCounts = countTickets(ticketRequests);
    validateTicketCounts(ticketCounts);

    final BigDecimal totalCost = calculateTotalCost(ticketCounts);
    final long seatCount = calculateSeatCount(ticketCounts);

    paymentService.debitAccount(accountId, totalCost);
    seatReservationService.reserveSeats(accountId, seatCount);

    return new BookingConfirmation(accountId);
  }

  private static void validateAccountId(final Long accountId) {
    if (accountId == null || accountId <= 0) {
      throw new InvalidBookingException("Account id must be greater than zero");
    }
  }

  private static Map<TicketType, Integer> countTickets(final TicketRequest... ticketRequests) {
    if (ticketRequests == null || ticketRequests.length == 0) {
      throw new InvalidBookingException("At least one ticket request is required");
    }

    final Map<TicketType, Integer> ticketCounts = new EnumMap<>(TicketType.class);

    for (TicketRequest ticketRequest : ticketRequests) {
      if (ticketRequest == null || ticketRequest.type() == null) {
        throw new InvalidBookingException("Ticket requests must include a ticket type");
      }
      if (ticketRequest.ticketCount() <= 0) {
        throw new InvalidBookingException("Ticket count must be greater than zero");
      }

      ticketCounts.merge(ticketRequest.type(), ticketRequest.ticketCount(), Integer::sum);
    }

    return ticketCounts;
  }

  private static void validateTicketCounts(final Map<TicketType, Integer> ticketCounts) {
    final int adultTickets = ticketCounts.getOrDefault(TicketType.ADULT, 0);
    final int childTickets = ticketCounts.getOrDefault(TicketType.CHILD, 0);
    final int infantTickets = ticketCounts.getOrDefault(TicketType.INFANT, 0);
    final int totalTickets = adultTickets + childTickets + infantTickets;

    if (totalTickets > MAXIMUM_TICKETS_PER_PURCHASE) {
      throw new InvalidBookingException("A maximum of 25 tickets can be purchased");
    }

    if (adultTickets == 0 && (childTickets > 0 || infantTickets > 0)) {
      throw new InvalidBookingException("Child and infant tickets require an adult ticket");
    }
  }

  private static BigDecimal calculateTotalCost(final Map<TicketType, Integer> ticketCounts) {
    final int adultTickets = ticketCounts.getOrDefault(TicketType.ADULT, 0);
    final int childTickets = ticketCounts.getOrDefault(TicketType.CHILD, 0);

    return ADULT_TICKET_PRICE.multiply(BigDecimal.valueOf(adultTickets))
        .add(CHILD_TICKET_PRICE.multiply(BigDecimal.valueOf(childTickets)));
  }

  private static long calculateSeatCount(final Map<TicketType, Integer> ticketCounts) {
    return (long) ticketCounts.getOrDefault(TicketType.ADULT, 0)
        + ticketCounts.getOrDefault(TicketType.CHILD, 0);
  }
}
