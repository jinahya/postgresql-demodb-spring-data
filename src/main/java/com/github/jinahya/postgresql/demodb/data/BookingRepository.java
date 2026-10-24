package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import com.github.jinahya.postgresql.demodb.persistence.Booking_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A repository for {@link Booking}, the {@code bookings} table of the demonstration database.
 * <p>
 * The id is the booking reference, a {@link String} assigned by whoever creates the row. The methods below answer
 * the exercises of upstream's {@code EXERCISES.asciidoc} on the {@code bookings} table, one method per exercise,
 * named in each method's javadoc by the exercise's id ({@code DEMODB_BOOKINGS_B_01}, ...). A Basic exercise is a
 * derived query, with the order the exercise asks for in its name, and returns a {@link List}, with no
 * {@link Pageable}. An Intermediate one takes a {@link Pageable}, the page, and returns a {@link Page}; the order the
 * exercise asks for is in the query, and its {@code LIMIT} is the size of the caller's page. A result that is not an
 * entity comes back as a {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see Booking
 * @see Booking_
 */
@Repository
public interface BookingRepository
        extends _Repository<Booking, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the bookings made in the specified half-open interval, by {@link Booking_#BOOK_DATE}
     * ({@code DEMODB_BOOKINGS_B_01}, which asks for the bookings of the first minute of the data,
     * {@code 2025-09-01 00:00} inclusive to {@code 00:01} exclusive, ordered by {@link Booking_#BOOK_DATE}).
     * <p>
     * The query, and its order, are derived from the method name. The exercise lists the matches whole, so there is
     * no {@link Pageable}.
     *
     * @param from the lower bound of the {@link Booking_#BOOK_DATE}, inclusive; must not be {@code null}.
     * @param to   the upper bound of the {@link Booking_#BOOK_DATE}, exclusive; must not be {@code null}.
     * @return a list of the bookings made in {@code [from, to)}, by their dates; empty if there is none.
     */
    List<Booking> findAllByBookDateGreaterThanEqualAndBookDateLessThanOrderByBookDateAsc(OffsetDateTime from,
                                                                                          OffsetDateTime to);

    /**
     * Selects each month with how many bookings were made in it, and for how much in all
     * ({@code DEMODB_BOOKINGS_I_01}, which orders by the month; the exercise shows every row, so the caller's page
     * holds them all).
     * <p>
     * The year and the month are taken out of the {@link Booking_#BOOK_DATE} with {@code EXTRACT}, in the session's
     * time zone, and the bookings are grouped by the two, counted, and their {@link Booking_#TOTAL_AMOUNT}s summed.
     * The count query counts the distinct months through the same expressions.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the months with their numbers of bookings and their totals, each a tuple of {@code year} and
     *         {@code month} ({@link Integer}s), {@code bookings} ({@link Long}) and {@code total}
     *         ({@link java.math.BigDecimal}).
     */
    @Query(value = """
            SELECT EXTRACT(YEAR FROM b.bookDate) AS year,
                   EXTRACT(MONTH FROM b.bookDate) AS month,
                   COUNT(b) AS bookings,
                   SUM(b.totalAmount) AS total
            FROM Booking b
            GROUP BY EXTRACT(YEAR FROM b.bookDate), EXTRACT(MONTH FROM b.bookDate)
            ORDER BY EXTRACT(YEAR FROM b.bookDate), EXTRACT(MONTH FROM b.bookDate)""",
           countQuery = """
                   SELECT COUNT(DISTINCT EXTRACT(YEAR FROM b.bookDate) * 100 + EXTRACT(MONTH FROM b.bookDate))
                   FROM Booking b""")
    Page<Tuple> selectBookingCountsAndTotalsByMonth(Pageable pageable);
}
