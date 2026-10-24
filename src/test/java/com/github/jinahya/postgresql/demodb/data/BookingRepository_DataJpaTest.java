package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link BookingRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the bookings it needs through the fixtures,
 * sets the dates and the amounts it is about, and the slice rolls them back when it ends. The dates are mid-month
 * instants in UTC where a month matters, so that the month a query takes out of them is the same whatever time zone
 * the H2 session has.
 *
 * @see BookingRepository_SpringBootIT
 */
class BookingRepository_DataJpaTest
        extends _Repository_DataJpaTest<BookingRepository, Booking, String> {

    private static final OffsetDateTime MINUTE = OffsetDateTime.of(2031, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    /**
     * Creates a new instance.
     */
    BookingRepository_DataJpaTest() {
        super(BookingRepository.class, Booking.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a randomized booking made at the specified instant, for the specified amount.
     *
     * @param bookDate    the date of the booking.
     * @param totalAmount the amount of the booking.
     * @return the saved booking, managed in the test's persistence context.
     */
    private Booking saveBooking(final OffsetDateTime bookDate, final String totalAmount) {
        final var booking = fixtures().booking();
        booking.setBookDate(bookDate);
        booking.setTotalAmount(new BigDecimal(totalAmount));
        return booking;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link BookingRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().booking();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
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
         * Asserts that a minute no booking was made in finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    MINUTE, MINUTE.plusMinutes(1L));
            assertThat(found).isEmpty();
        }

        /**
         * Saves three bookings in a minute, out of order, and one in the next, and asserts that the minute finds the
         * three, those very instances, by date as the exercise sorts, the minute's end excluded.
         */
        @Test
        void __() {
            final var third = saveBooking(MINUTE.plusSeconds(40L), "100.00");
            final var first = saveBooking(MINUTE, "200.00");
            final var second = saveBooking(MINUTE.plusSeconds(20L), "300.00");
            final var next = saveBooking(MINUTE.plusMinutes(1L), "400.00");
            final var found = repositoryInstance().findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    MINUTE, MINUTE.plusMinutes(1L));
            assertThat(found)
                    .containsExactly(first, second, third)
                    .doesNotContain(next);
            assertThat(found).anySatisfy(b -> assertThat(b).isSameAs(first));
        }

        /**
         * Asserts that a {@code null} bound is rejected before any query runs: the package is {@code @NullMarked},
         * so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository.findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    null, MINUTE))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository.findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(
                    MINUTE, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link BookingRepository#selectBookingCountsAndTotalsByMonth(Pageable)} ({@code DEMODB_BOOKINGS_I_01}).
     */
    @Nested
    class SelectBookingCountsAndTotalsByMonth_Test {

        /**
         * Saves two bookings in one month and one in another, later, month, and asserts that the two months are
         * selected with their counts and their totals, by month as the exercise sorts.
         */
        @Test
        void __() {
            saveBooking(OffsetDateTime.of(2031, 5, 10, 12, 0, 0, 0, ZoneOffset.UTC), "10.00");
            saveBooking(OffsetDateTime.of(2031, 3, 15, 12, 0, 0, 0, ZoneOffset.UTC), "100.00");
            saveBooking(OffsetDateTime.of(2031, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC), "250.50");
            final var page = repositoryInstance().selectBookingCountsAndTotalsByMonth(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(intColumn("year"), intColumn("month"), longColumn("bookings"))
                    .containsExactly(tuple(2031, 3, 2L), tuple(2031, 5, 1L));
            assertThat(page.getContent())
                    .extracting(column("total", BigDecimal.class))
                    .satisfiesExactly(
                            t -> assertThat(t).isEqualByComparingTo("350.50"),
                            t -> assertThat(t).isEqualByComparingTo("10.00"));
        }

        /**
         * Saves a booking in each of three months, and asserts that pages of two split the months two and one, three
         * in all, which the count query says.
         */
        @Test
        void __Paged() {
            for (int month = 1; month <= 3; month++) {
                saveBooking(OffsetDateTime.of(2031, month, 15, 12, 0, 0, 0, ZoneOffset.UTC), "10.00");
            }
            final var page0 = repositoryInstance().selectBookingCountsAndTotalsByMonth(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectBookingCountsAndTotalsByMonth(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
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
