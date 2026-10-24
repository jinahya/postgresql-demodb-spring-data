package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import com.github.jinahya.postgresql.demodb.persistence.Seat_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link Seat}, the {@code seats} table of the demonstration database.
 * <p>
 * The id is {@link SeatId}, the airplane's code and the seat number, assigned through {@code Seat.of(...)}. The
 * methods below answer the exercises of upstream's {@code EXERCISES.asciidoc} on the {@code seats} table, one
 * method per exercise, named in each method's javadoc by the exercise's id ({@code DEMODB_SEATS_B_01}, ...). A Basic
 * exercise is a derived query, with the order the exercise asks for in its name, and returns a {@link List}, with
 * no {@link Pageable}. An Intermediate one takes a {@link Pageable}, the page, and returns a {@link Page}; the order
 * the exercise asks for is in the query, and its {@code LIMIT} is the size of the caller's page. Such a query selects
 * the entity whole ({@code SELECT a AS airplane}); what is computed is a column of its own beside it, and the result
 * comes back as a {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see Seat
 * @see Seat_
 */
@Repository
public interface SeatRepository
        extends _Repository<Seat, SeatId> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the seats of the specified airplane in the specified fare conditions, by {@link Seat_#SEAT_NO}
     * ({@code DEMODB_SEATS_B_01}, which asks for the Business seats of the Airbus A320neo, {@code 32N}, ordered by
     * {@link Seat_#SEAT_NO}).
     * <p>
     * The query, and its order, are derived from the method name, over the {@link Seat_#AIRPLANE_CODE} part of the
     * id. The seat numbers are text, so {@code 10A} comes before {@code 1A}, as the exercise's output has it. The
     * exercise lists the matches whole, so there is no {@link Pageable}.
     *
     * @param airplaneCode   the value of the {@link Seat_#AIRPLANE_CODE} attribute to match; must not be
     *                       {@code null}.
     * @param fareConditions the value of the {@link Seat_#FARE_CONDITIONS} attribute to match; must not be
     *                       {@code null}.
     * @return a list of the seats of {@code airplaneCode} in {@code fareConditions}, by seat number; empty if there
     *         is none.
     */
    List<Seat> findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(String airplaneCode, String fareConditions);

    /**
     * Selects every airplane with how many seats it has in each fare conditions, and in all
     * ({@code DEMODB_SEATS_I_01}, which orders by the airplane's code; the exercise shows every row, so the caller's
     * page holds them all).
     * <p>
     * The seats are joined to their airplane, from the airplane's side and left, so that an airplane with no seat
     * is a row of zeros, grouped by the airplane itself, which selects the airplane whole, and counted by fare
     * conditions through a {@code CASE} summed, the exercise's {@code FILTER}, and in all through the seat numbers,
     * which a left join leaves {@code null} where there is no seat, as the exercise counts them. The count query
     * counts the airplanes.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the airplanes with their seat counts, each a tuple of {@code airplane}
     *         ({@link com.github.jinahya.postgresql.demodb.persistence.AirplaneData}), {@code economy},
     *         {@code comfort}, {@code business} and {@code seats} ({@link Long}s).
     */
    @Query(value = """
            SELECT a AS airplane,
                   SUM(CASE WHEN s.fareConditions = 'Economy' THEN 1 ELSE 0 END) AS economy,
                   SUM(CASE WHEN s.fareConditions = 'Comfort' THEN 1 ELSE 0 END) AS comfort,
                   SUM(CASE WHEN s.fareConditions = 'Business' THEN 1 ELSE 0 END) AS business,
                   COUNT(s.seatNo) AS seats
            FROM AirplaneData a
                     LEFT JOIN Seat s ON s.airplaneData = a
            GROUP BY a
            ORDER BY a.airplaneCode""",
           countQuery = """
                   SELECT COUNT(a)
                   FROM AirplaneData a""")
    Page<Tuple> selectSeatCountsByFareConditions(Pageable pageable);
}
