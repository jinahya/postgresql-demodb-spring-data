package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link FlightRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code flights_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById} round
 * trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database loaded;
 * see that file.
 *
 * @see FlightRepository_DataJpaTest
 */
class FlightRepository_SpringBootIT
        extends _Repository_SpringBootIT<FlightRepository, Flight, Integer> {

    private static final OffsetDateTime OCTOBER = OffsetDateTime.of(2025, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime NOVEMBER = OCTOBER.plusMonths(1L);

    /**
     * Creates a new instance.
     */
    FlightRepository_SpringBootIT() {
        super(FlightRepository.class, Flight.class, Integer.class);
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
         * Asserts that the exercise's route, {@code PG0001}, and month, October 2025, find the twenty-six flights the
         * exercise's expected output shows, the first ten by scheduled departure.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance()
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "PG0001", OCTOBER, NOVEMBER);
            assertThat(found).hasSize(26);
            assertThat(found.subList(0, 10))
                    .extracting(Flight::getFlightId)
                    .containsExactly(56, 420, 595, 786, 962, 1145, 1319, 1682, 1858, 2049);
            assertThat(found.getFirst().getScheduledDeparture())
                    .isEqualTo(OffsetDateTime.of(2025, 10, 1, 8, 55, 0, 0, ZoneOffset.UTC));
            assertThat(found).allSatisfy(f -> assertThat(f.getStatus()).isEqualTo("Arrived"));
        }

        /**
         * Asserts that a route and October find flights of that route scheduled in October only, sorted by
         * scheduled departure.
         *
         * @param routeNo the route number.
         */
        @ValueSource(strings = {"PG0001", "PG0006", "PG0012"})
        @ParameterizedTest
        void __(final String routeNo) {
            final var found = repositoryInstance()
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            routeNo, OCTOBER, NOVEMBER);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(f -> {
                        assertThat(f.getRouteNo()).isEqualTo(routeNo);
                        assertThat(f.getScheduledDeparture()).isAfterOrEqualTo(OCTOBER).isBefore(NOVEMBER);
                    })
                    .isSortedAccordingTo(Comparator.comparing(Flight::getScheduledDeparture));
        }

        /**
         * Asserts that a {@code null} argument is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            null, OCTOBER, NOVEMBER))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "PG0001", null, NOVEMBER))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByRouteNoAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "PG0001", OCTOBER, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link FlightRepository#selectFlightCountsByMonth(Pageable)} ({@code DEMODB_FLIGHTS_I_01}).
     */
    @Nested
    class SelectFlightCountsByMonth_Test {

        /**
         * Asserts that the four months of the sample data come with their counts, by month, as the exercise's
         * expected output shows, four in all.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectFlightCountsByMonth(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(intColumn("year"), intColumn("month"), longColumn("flights"))
                    .containsExactly(
                            tuple(2025, 10, 5580L),
                            tuple(2025, 11, 5467L),
                            tuple(2025, 12, 5539L),
                            tuple(2026, 1, 5172L));
            assertThat(page.getTotalElements()).isEqualTo(4L);
        }

        /**
         * Asserts that the counts add up to every flight.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectFlightCountsByMonth(Pageable.unpaged());
            assertThat(page.getContent().stream().mapToLong(longColumn("flights")::apply).sum())
                    .isEqualTo(repositoryInstance().count());
        }

        /**
         * Asserts that pages of three walk the four months, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectFlightCountsByMonth(PageRequest.of(0, 3));
            assertThat(page0.getContent()).hasSize(3);
            assertThat(page0.getTotalElements()).isEqualTo(4L);
            final var page1 = repositoryInstance().selectFlightCountsByMonth(page0.nextPageable());
            assertThat(page1.getContent())
                    .extracting(intColumn("year"), intColumn("month"))
                    .containsExactly(tuple(2026, 1));
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
