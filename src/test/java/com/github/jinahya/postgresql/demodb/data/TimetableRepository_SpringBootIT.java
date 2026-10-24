package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link TimetableRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code timetable_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById} round
 * trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database loaded;
 * see that file.
 *
 * @see TimetableRepository_DataJpaTest
 */
class TimetableRepository_SpringBootIT
        extends _Repository_SpringBootIT<TimetableRepository, Timetable, Integer> {

    /**
     * The database's {@code bookings.now()}, the instant the exercises take for the present.
     */
    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * Creates a new instance.
     */
    TimetableRepository_SpringBootIT() {
        super(TimetableRepository.class, Timetable.class, Integer.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests
     * {@link TimetableRepository#findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(String, OffsetDateTime, OffsetDateTime)}
     * ({@code DEMODB_TIMETABLE_B_01}).
     */
    @Nested
    class FindAllByDepartureAirportAndScheduledDepartureBetween_Test {

        /**
         * Asserts that the exercise's airport, Sheremetyevo, {@code SVO}, and the 24 hours from
         * {@code bookings.now()} find the five flights the exercise's expected output shows, by scheduled departure.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance()
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "SVO", NOW, NOW.plusHours(24L));
            assertThat(found)
                    .extracting(Timetable::getFlightId, Timetable::getRouteNo, Timetable::getArrivalAirport,
                                t -> t.getScheduledDepartureLocal().toString(), Timetable::getStatus)
                    .containsExactly(
                            tuple(11120, "PG0012", "UFA", "2025-12-01T13:55", "On Time"),
                            tuple(11121, "PG0034", "SVX", "2025-12-01T14:05", "On Time"),
                            tuple(11132, "PG0177", "AER", "2025-12-01T15:05", "On Time"),
                            tuple(11211, "PG0018", "KZN", "2025-12-02T02:05", "On Time"),
                            tuple(11216, "PG0106", "LED", "2025-12-02T02:40", "On Time"));
        }

        /**
         * Asserts that an airport and a day find flights from that airport scheduled in that day only, sorted by
         * scheduled departure, each departing in Moscow's local time three hours after UTC.
         *
         * @param departureAirport the airport's code.
         */
        @ValueSource(strings = {"SVO", "DME", "LED"})
        @ParameterizedTest
        void __(final String departureAirport) {
            final var found = repositoryInstance()
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            departureAirport, NOW, NOW.plusDays(1L));
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(t -> {
                        assertThat(t.getDepartureAirport()).isEqualTo(departureAirport);
                        assertThat(t.getScheduledDeparture()).isAfterOrEqualTo(NOW).isBefore(NOW.plusDays(1L));
                        assertThat(t.getScheduledDepartureLocal())
                                .isEqualTo(t.getScheduledDeparture().atZoneSameInstant(ZoneOffset.ofHours(3))
                                                   .toLocalDateTime());
                    })
                    .isSortedAccordingTo(Comparator.comparing(Timetable::getScheduledDeparture));
        }

        /**
         * Asserts that a {@code null} argument is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            null, NOW, NOW.plusDays(1L)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "SVO", null, NOW.plusDays(1L)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "SVO", NOW, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link TimetableRepository#countDepartingOnAnotherLocalDay()} ({@code DEMODB_TIMETABLE_I_01}).
     */
    @Nested
    class CountDepartingOnAnotherLocalDay_Test {

        /**
         * Asserts that the count is the one the exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            assertThat(repositoryInstance().countDepartingOnAnotherLocalDay()).isEqualTo(4597L);
        }

        /**
         * Asserts that the count is positive and less than every flight: some flights depart on another local day,
         * and not all of them.
         */
        @Test
        void __() {
            final var count = repositoryInstance().countDepartingOnAnotherLocalDay();
            assertThat(count).isPositive().isLessThan(repositoryInstance().count());
        }
    }
}
