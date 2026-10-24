package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link FlightRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the flights it needs through the fixtures,
 * sets the route numbers and the scheduled departures it is about, and the slice rolls them back when it ends. The
 * departures are mid-month instants in UTC, so that the month a query takes out of them is the same whatever time
 * zone the H2 session has.
 *
 * @see FlightRepository_SpringBootIT
 */
class FlightRepository_DataJpaTest
        extends _Repository_DataJpaTest<FlightRepository, Flight, Integer> {

    /**
     * Creates a new instance.
     */
    FlightRepository_DataJpaTest() {
        super(FlightRepository.class, Flight.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns a route number no other test uses: {@code PG} and four random characters.
     *
     * @return a random route number.
     */
    private static String randomRouteNo() {
        return "PG" + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }

    /**
     * Returns the specified instant, in UTC.
     *
     * @param year  the year.
     * @param month the month.
     * @param day   the day.
     * @param hour  the hour.
     * @return the instant.
     */
    private static OffsetDateTime utc(final int year, final int month, final int day, final int hour) {
        return OffsetDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    /**
     * Saves a randomized flight of the specified route scheduled to depart at the specified instant, and to arrive
     * two hours later, with no actual times.
     *
     * @param routeNo   the route number.
     * @param departure the scheduled departure.
     * @return the saved flight, managed in the test's persistence context.
     */
    private Flight saveFlight(final String routeNo, final OffsetDateTime departure) {
        final var flight = _Repository_DataJpaTest_Fixtures.randomized(Flight.class);
        flight.setRouteNo(routeNo);
        flight.setScheduledDeparture(departure);
        flight.setScheduledArrival(departure.plusHours(2L));
        flight.setActualDeparture(null);
        flight.setActualArrival(null);
        return repositoryInstance().save(flight);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link FlightRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().flight();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests
     * {@link FlightRepository#findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(String, OffsetDateTime, OffsetDateTime)}
     * ({@code DEMODB_FLIGHTS_B_01}).
     */
    @Nested
    class FindAllByRouteNoAndScheduledDepartureBetween_Test {

        /**
         * Asserts that a route no flight is of finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance()
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            randomRouteNo(), utc(2031, 10, 1, 0), utc(2031, 11, 1, 0));
            assertThat(found).isEmpty();
        }

        /**
         * Saves three flights of a route in October, out of order, one in November, and one of another route in
         * October, and asserts that the route and October find the three, those very instances, by scheduled
         * departure as the exercise sorts, the month's end excluded.
         */
        @Test
        void __() {
            final var routeNo = randomRouteNo();
            final var twentieth = saveFlight(routeNo, utc(2031, 10, 20, 12));
            final var fifth = saveFlight(routeNo, utc(2031, 10, 5, 12));
            final var first = saveFlight(routeNo, utc(2031, 10, 1, 0));
            final var november = saveFlight(routeNo, utc(2031, 11, 1, 0));
            final var other = saveFlight(randomRouteNo(), utc(2031, 10, 10, 12));
            final var found = repositoryInstance()
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            routeNo, utc(2031, 10, 1, 0), utc(2031, 11, 1, 0));
            assertThat(found)
                    .containsExactly(first, fifth, twentieth)
                    .doesNotContain(november, other);
            assertThat(found).anySatisfy(f -> assertThat(f).isSameAs(first));
        }

        /**
         * Asserts that a {@code null} argument is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            null, utc(2031, 10, 1, 0), utc(2031, 11, 1, 0)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            randomRouteNo(), null, utc(2031, 11, 1, 0)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            randomRouteNo(), utc(2031, 10, 1, 0), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link FlightRepository#selectFlightCountsByMonth(Pageable)} ({@code DEMODB_FLIGHTS_I_01}).
     */
    @Nested
    class SelectFlightCountsByMonth_Test {

        /**
         * Saves two flights in one month and one in another, later, month, and asserts that the two months are
         * selected with their counts, by month as the exercise sorts.
         */
        @Test
        void __() {
            saveFlight(randomRouteNo(), utc(2031, 5, 10, 12));
            saveFlight(randomRouteNo(), utc(2031, 3, 15, 12));
            saveFlight(randomRouteNo(), utc(2031, 3, 20, 12));
            final var page = repositoryInstance().selectFlightCountsByMonth(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(intColumn("year"), intColumn("month"), longColumn("flights"))
                    .containsExactly(tuple(2031, 3, 2L), tuple(2031, 5, 1L));
        }

        /**
         * Saves a flight in each of three months, and asserts that pages of two split the months two and one, three
         * in all, which the count query says.
         */
        @Test
        void __Paged() {
            for (int month = 1; month <= 3; month++) {
                saveFlight(randomRouteNo(), utc(2031, month, 15, 12));
            }
            final var page0 = repositoryInstance().selectFlightCountsByMonth(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectFlightCountsByMonth(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectFlightCountsByMonth(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
