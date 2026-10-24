package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
import com.github.jinahya.postgresql.demodb.persistence.Segment_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link Segment}, the {@code segments} table of the demonstration database.
 * <p>
 * The id is {@link SegmentId}, the ticket number and the flight id, assigned through {@code Segment.of(...)}. The
 * methods below answer the exercises of upstream's {@code EXERCISES.asciidoc} on the {@code segments} table, one
 * method per exercise, named in each method's javadoc by the exercise's id ({@code DEMODB_SEGMENTS_B_01}, ...). A
 * Basic exercise is a derived query, with the order the exercise asks for in its name, and returns a {@link List},
 * with no {@link Pageable}. An Intermediate one takes a {@link Pageable}, the page, and returns a {@link Page}; the
 * order the exercise asks for is in the query, and its {@code LIMIT} is the size of the caller's page. A result that
 * is not an entity comes back as a {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see Segment
 * @see Segment_
 */
@Repository
public interface SegmentRepository
        extends _Repository<Segment, SegmentId> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the segments of the specified ticket, by {@link Segment_#FLIGHT_ID} ({@code DEMODB_SEGMENTS_B_01}, which
     * asks for the segments of the ticket {@code 0005432000000}, ordered by {@link Segment_#FLIGHT_ID}).
     * <p>
     * The query, and its order, are derived from the method name, over the two parts of the id. The exercise lists
     * the matches whole, so there is no {@link Pageable}.
     *
     * @param ticketNo the value of the {@link Segment_#TICKET_NO} attribute to match; must not be {@code null}.
     * @return a list of the segments of {@code ticketNo}, by flight id; empty if there is none.
     */
    List<Segment> findAllByTicketNoOrderByFlightIdAsc(String ticketNo);

    /**
     * Selects each fare conditions with the cheapest, the average and the dearest price paid for a segment in it
     * ({@code DEMODB_SEGMENTS_I_01}, which orders by the fare conditions; the exercise shows every row, so the
     * caller's page holds them all).
     * <p>
     * The segments are grouped by {@link Segment_#FARE_CONDITIONS} and their {@link Segment_#PRICE}s aggregated. The
     * exercise rounds the average to two places, which a caller does when it shows it. The count query counts the
     * distinct fare conditions.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the fare conditions with their prices, each a tuple of {@code fareConditions}
     *         ({@link String}), {@code cheapest} and {@code dearest} ({@link java.math.BigDecimal}s) and
     *         {@code average} (a {@link Double}, as the provider gives an average).
     */
    @Query(value = """
            SELECT s.fareConditions AS fareConditions,
                   MIN(s.price) AS cheapest,
                   AVG(s.price) AS average,
                   MAX(s.price) AS dearest
            FROM Segment s
            GROUP BY s.fareConditions
            ORDER BY s.fareConditions""",
           countQuery = """
                   SELECT COUNT(DISTINCT s.fareConditions)
                   FROM Segment s""")
    Page<Tuple> selectPriceStatisticsByFareConditions(Pageable pageable);
}
