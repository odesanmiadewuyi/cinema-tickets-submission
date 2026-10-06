package uk.gov.dwp.engineering.recruitment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookingConfirmationTest {

  @Test
  void givenBookingConfirmation_whenGetAccount_thenExpectedValueReturned() {
    final BookingConfirmation bookingConfirmation = new BookingConfirmation(99L);
    assertEquals(99L, bookingConfirmation.accountId());
  }
}
