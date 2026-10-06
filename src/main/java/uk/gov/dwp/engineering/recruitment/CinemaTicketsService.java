package uk.gov.dwp.engineering.recruitment;

import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

public interface CinemaTicketsService {

  BookingConfirmation purchaseTickets(final Long accountId, final TicketRequest... ticketRequests)
      throws InvalidBookingException;
}
