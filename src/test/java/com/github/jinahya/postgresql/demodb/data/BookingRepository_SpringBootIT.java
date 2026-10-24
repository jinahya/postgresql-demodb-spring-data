package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link BookingRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code bookings_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById} round
 * trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database loaded;
 * see that file.
 *
 * @see BookingRepository_DataJpaTest
 */
class BookingRepository_SpringBootIT
        extends _Repository_SpringBootIT<BookingRepository, Booking, String> {

    /**
     * The first minute of the data.
     */
    private static final OffsetDateTime FIRST = OffsetDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    /**
     * Creates a new instance.
     */
    BookingRepository_SpringBootIT() {
        super(BookingRepository.class, Booking.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests
     * {@link BookingRepository#findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(OffsetDateTime, OffsetDateTime)}
     * ({@code DEMODB_BOOKINGS_B_01}).
     */
    @Nested
    class FindAllByBookDateBetween_Test {

        /**
         * Asserts that the exercise's minute, the first of the data, finds the seventeen bookings the exercise's
         * expected output shows, the first ten by date.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    FIRST, FIRST.plusMinutes(1L));
            assertThat(found).hasSize(17);
            assertThat(found.subList(0, 10))
                    .extracting(Booking::getBookRef)
                    .containsExactly("6TAS39", "GGY5EU", "Y4KNLB", "MJUZ2D", "4O2NMP", "2EW1SQ", "KP3OIQ", "H40481",
                                     "KOS1KJ", "NWE9DB");
            assertThat(found.getFirst().getTotalAmount()).isEqualByComparingTo("5000.00");
        }

        /**
         * Asserts that a minute of the first day finds bookings made in that minute only, sorted by date.
         *
         * @param minute the minute of the first day.
         */
        @ValueSource(ints = {0, 30, 600})
        @ParameterizedTest
        void __(final int minute) {
            final var from = FIRST.plusMinutes(minute);
            final var to = from.plusMinutes(1L);
            final var found = repositoryInstance().findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    from, to);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(b -> assertThat(b.getBookDate()).isAfterOrEqualTo(from).isBefore(to))
                    .isSortedAccordingTo(Comparator.comparing(Booking::getBookDate));
        }

        /**
         * Asserts that a {@code null} bound is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository.findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    null, FIRST))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository.findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    FIRST, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link BookingRepository#selectBookingCountsAndTotalsByMonth(Pageable)} ({@code DEMODB_BOOKINGS_I_01}).
     */
    @Nested
    class SelectBookingCountsAndTotalsByMonth_Test {

        /**
         * Asserts that the three months of the sample data come with their counts and their totals, by month, as
         * the exercise's expected output shows, three in all.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectBookingCountsAndTotalsByMonth(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(intColumn("year"), intColumn("month"), longColumn("bookings"))
                    .containsExactly(
                            tuple(2025, 9, 448064L),
                            tuple(2025, 10, 434159L),
                            tuple(2025, 11, 410670L));
            assertThat(page.getContent())
                    .extracting(column("total", BigDecimal.class))
                    .satisfiesExactly(
                            t -> assertThat(t).isEqualByComparingTo("8749891200.00"),
                            t -> assertThat(t).isEqualByComparingTo("8223964900.00"),
                            t -> assertThat(t).isEqualByComparingTo("7600075475.00"));
            assertThat(page.getTotalElements()).isEqualTo(3L);
        }

        /**
         * Asserts that the counts add up to every booking.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectBookingCountsAndTotalsByMonth(Pageable.unpaged());
            assertThat(page.getContent().stream().mapToLong(longColumn("bookings")::apply).sum())
                    .isEqualTo(repositoryInstance().count());
        }

        /**
         * Asserts that pages of two walk the three months, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectBookingCountsAndTotalsByMonth(page0.nextPageable());
            assertThat(page1.getContent())
                    .extracting(intColumn("year"), intColumn("month"))
                    .containsExactly(tuple(2025, 11));
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectBookingCountsAndTotalsByMonth(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
