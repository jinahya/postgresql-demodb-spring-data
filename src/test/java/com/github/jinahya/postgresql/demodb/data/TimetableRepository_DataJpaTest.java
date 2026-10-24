package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link TimetableRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. On the H2 the {@code timetable} view is a table generated from
 * its entity, whose columns, the flight's id apart, are read-only, so a row saved through the fixtures carries its
 * id and nothing else; the exercises therefore run against rows with no airport and no time, which still proves
 * their queries valid and runnable on the H2.
 *
 * @see TimetableRepository_SpringBootIT
 */
class TimetableRepository_DataJpaTest
        extends _Repository_DataJpaTest<TimetableRepository, Timetable, Integer> {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2031, 12, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * Creates a new instance.
     */
    TimetableRepository_DataJpaTest() {
        super(TimetableRepository.class, Timetable.class, Integer.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link TimetableRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().timetable();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
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
         * Saves a row, whose airport and times the H2 cannot hold, and asserts that an airport and a day find an
         * empty list: the query runs, and matches no row.
         */
        @Test
        void _Empty_Unknown() {
            fixtures().timetable();
            final var found = repositoryInstance()
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "svo", NOW, NOW.plusDays(1L));
            assertThat(found).isEmpty();
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
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            null, NOW, NOW.plusDays(1L)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "svo", null, NOW.plusDays(1L)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository
                    .findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
                            "svo", NOW, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link TimetableRepository#countDepartingOnAnotherLocalDay()} ({@code DEMODB_TIMETABLE_I_01}).
     */
    @Nested
    class CountDepartingOnAnotherLocalDay_Test {

        /**
         * Saves a row, whose times the H2 cannot hold, and asserts that the count, with its two {@code EXTRACT}s,
         * runs on the H2 and counts nothing: a row with no times has no days to differ.
         */
        @Test
        void _Zero_NoTimes() {
            fixtures().timetable();
            assertThat(repositoryInstance().countDepartingOnAnotherLocalDay()).isZero();
        }
    }
}
