package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Timetable;
import com.github.jinahya.postgresql.demodb.persistence.Timetable_;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A repository for {@link Timetable}, the {@code timetable} view of the demonstration database.
 * <p>
 * The view shows the {@code flights} table with its routes' airports, and each scheduled and actual time both as an
 * instant and in the local time of its airport, so every column but the id, the flight's id, is read-only. The
 * methods below answer the exercises of upstream's {@code EXERCISES.asciidoc} on the {@code timetable} view, one
 * method per exercise, named in each method's javadoc by the exercise's id ({@code DEMODB_TIMETABLE_B_01}, ...). A
 * Basic exercise is a derived query, with the order the exercise asks for in its name, and returns a {@link List},
 * with no {@link Pageable}. An Intermediate one that counts is a {@code count...} method with its JPQL declared by
 * {@link Query}.
 *
 * @see Timetable
 * @see Timetable_
 */
@Repository
public interface TimetableRepository
        extends _Repository<Timetable, Integer> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the flights from the specified airport scheduled to depart in the specified half-open interval, by
     * {@link Timetable_#SCHEDULED_DEPARTURE} ({@code DEMODB_TIMETABLE_B_01}, which asks for the flights from
     * Sheremetyevo, {@code SVO}, in the 24 hours from the database's {@code bookings.now()},
     * {@code 2025-12-01 00:00:00+00}, ordered by {@link Timetable_#SCHEDULED_DEPARTURE}).
     * <p>
     * The query, and its order, are derived from the method name; the exercise's {@code now}, and the 24 hours after
     * it, are the caller's bounds. The exercise lists the matches whole, so there is no {@link Pageable}.
     *
     * @param departureAirport the value of the {@link Timetable_#DEPARTURE_AIRPORT} attribute to match; must not be
     *                         {@code null}.
     * @param from             the lower bound of the {@link Timetable_#SCHEDULED_DEPARTURE}, inclusive; must not be
     *                         {@code null}.
     * @param to               the upper bound of the {@link Timetable_#SCHEDULED_DEPARTURE}, exclusive; must not be
     *                         {@code null}.
     * @return a list of the flights from {@code departureAirport} scheduled to depart in {@code [from, to)}, by
     *         scheduled departure; empty if there is none.
     */
    List<Timetable> findAllByDepartureAirportAndScheduledDepartureGreaterThanEqualAndScheduledDepartureLessThanOrderByScheduledDepartureAsc(
            String departureAirport, OffsetDateTime from, OffsetDateTime to);

    /**
     * Counts the flights scheduled to depart on another day in their airport's local time than in UTC
     * ({@code DEMODB_TIMETABLE_I_01}, a single value, which has no order).
     * <p>
     * A local day differs from the UTC day by at most one, so the two differ exactly when their days of the month
     * do; {@code EXTRACT(DAY ...)} of the {@link Timetable_#SCHEDULED_DEPARTURE_LOCAL} is compared with that of the
     * {@link Timetable_#SCHEDULED_DEPARTURE}, which the session reads in UTC.
     *
     * @return the number of the flights scheduled to depart on another local day than the UTC one.
     */
    @Query("""
            SELECT COUNT(t)
            FROM Timetable t
            WHERE EXTRACT(DAY FROM t.scheduledDepartureLocal) <> EXTRACT(DAY FROM t.scheduledDeparture)""")
    long countDepartingOnAnotherLocalDay();
}
