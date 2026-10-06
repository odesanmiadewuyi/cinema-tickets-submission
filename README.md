# Cinema Tickets Code Test

## Overview

Java implementation of the cinema ticket booking exercise. The service validates
each purchase, calculates the payment amount, reserves the required seats, and
returns a booking confirmation.

The project targets **Java 21** and uses Spring Boot and Maven. Compilation and
tests were verified using JDK 24 with Java 21 as the compilation target.

## Requirements

- JDK 21 or above, with `JAVA_HOME` pointing to the JDK installation directory.
- Maven available on `PATH` (verified using Maven 3.9.16).
- Internet access for the initial Maven dependency downloads.

Check the Java version used by Maven before building:

```powershell
mvn -version
```

If Maven selects an older JDK, update `JAVA_HOME`. For example, on a Windows
machine with JDK 24 installed at the following location:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-24'
```

This command changes the current PowerShell session only. For a permanent change,
update the Windows user environment variable and restart the terminal or IDE.

## Build and Test

Run these commands from the project directory containing `pom.xml`:

```powershell
mvn test
mvn clean verify
```

`mvn test` runs the unit tests. `mvn clean verify` rebuilds the project, runs tests,
packages the application, and executes the configured verification phases.

The latest verification completed with `BUILD SUCCESS`: **17 tests passed**, with
zero failures, errors, or skipped tests. No integration tests are currently
defined, so Maven Failsafe reports `No tests to run`.

## Business Rules

| Ticket type | Price (GBP) | Seat allocated |
| --- | --- | --- |
| ADULT | 25.99 | Yes |
| CHILD | 17.50 | Yes |
| INFANT | 0.00 | No |

- A purchase may contain multiple ticket types and quantities.
- A maximum of 25 tickets may be purchased in one transaction.
- Child and infant tickets require an adult ticket in the same purchase.
- Infants are not charged and do not receive a seat; they sit on an adult's lap.
- An account ID must be greater than zero.
- Invalid bookings raise `InvalidBookingException` before payment or reservation.

## Assumptions and Ambiguities

- The 25-ticket limit includes infants, even though they are free and receive no
  seat, because the rule limits tickets rather than seats or paid tickets.
- Each supplied ticket request must have a positive quantity. A ticket type that
  is not needed should be omitted rather than supplied with a zero quantity.
- Null or empty request arrays, null request entries, and null ticket types are
  rejected as invalid input.
- Repeated requests for the same ticket type are combined into one quantity.
- The adult requirement is interpreted as at least one adult in the purchase.
  The specification does not explicitly state a one-infant-per-adult limit, so
  the implementation does not enforce that ratio. If one infant per adult is
  intended, additional validation and tests would be required.

The supplied exercise assumptions are retained: positive account IDs are valid
and have sufficient funds, and payment and reservation providers are assumed to
complete successfully. Provider failures, refunds, and rollback between payment
and reservation are outside the exercise scope.

## Implementation Approach

`CinemaTicketsServiceImpl.purchaseTickets` performs the following steps:

1. Validate the account ID and each ticket request.
2. Aggregate quantities by `TicketType` using an `EnumMap`.
3. Validate the total ticket limit and adult requirement.
4. Calculate the charge using `BigDecimal` for decimal monetary arithmetic.
5. Calculate seats as the number of adult tickets plus child tickets.
6. Call `PaymentService.debitAccount`, then `SeatReservationService.reserveSeats`.
7. Return a `BookingConfirmation` containing the account ID.

For example, two adults, three children, and one infant cost
`(2 x 25.99) + (3 x 17.50) = GBP 104.48`. This purchase contains six tickets
and requires five seats.

The `CinemaTicketsService` interface and the domain classes are unchanged.
Invalid booking exceptions are mapped to HTTP `400 Bad Request` by the REST
exception handler.

## Test Coverage

Seven service tests use JUnit and Mockito to check:

- Payment, seat count, and confirmation for an adult/child/infant booking.
- Aggregation of repeated requests for the same ticket type.
- Rejection of child-only and infant-only bookings.
- Rejection of purchases exceeding 25 tickets.
- Rejection of a zero account ID and a zero ticket quantity.
- No payment or reservation calls for the invalid bookings above.

The remaining tests cover application startup, domain record accessors, the
booking exception, and the supplied third-party service implementations.
The suite does not currently provide REST endpoint or end-to-end integration
coverage, or tests for every input validation branch and boundary condition.

## Run the Application

```powershell
mvn spring-boot:run
```

Alternatively, after `mvn clean verify`:

```powershell
java -jar target/cinema-tickets-0.0.2-SNAPSHOT.jar
```

The booking endpoint is `POST http://localhost:8080/cinema/bookings`. An example
JSON request is:

```json
{
  "accountId": 123,
  "ticketRequests": [
    { "type": "ADULT", "ticketCount": 2 },
    { "type": "CHILD", "ticketCount": 3 },
    { "type": "INFANT", "ticketCount": 1 }
  ]
}
```

A successful booking returns HTTP `201 Created` with:

```json
{
  "accountId": 123
}
```

The supplied payment and reservation implementations are placeholders that return
HTTP `200 OK` without making external calls. Local bookings therefore do not
require external providers, but do not debit a real account or reserve real seats.
The provider URLs in `src/main/resources/application.properties` are configuration
for future integration. The service unit tests use Mockito mocks to verify the
amount and seat count passed to the provider interfaces.
