package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPass_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link BoardingPass}, the {@code boarding_passes} table of the demonstration database.
 * <p>
 * The id is {@link BoardingPassId}, the ticket number and the flight id of the segment the pass boards, assigned
 * through {@code BoardingPass.of(...)}. The methods below answer the exercises of upstream's
 * {@code EXERCISES.asciidoc} on the {@code boarding_passes} table, one method per exercise, named in each method's
 * javadoc by the exercise's id ({@code DEMODB_BOARDING_PASSES_B_01}, ...). A Basic exercise is a derived query, with
 * the order the exercise asks for in its name, and returns a {@link List}, with no {@link Pageable}. An Intermediate
 * one takes a {@link Pageable}, the page, and returns a {@link Page}; the order the exercise asks for is in the
 * query, and its {@code LIMIT} is the size of the caller's page. A result that is not an entity comes back as a
 * {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see BoardingPass
 * @see BoardingPass_
 */
@Repository
public interface BoardingPassRepository
        extends _Repository<BoardingPass, BoardingPassId> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the boarding passes of the specified flight, in boarding order ({@code DEMODB_BOARDING_PASSES_B_01},
     * which asks for the passes of the flight {@code 1}, ordered by {@link BoardingPass_#BOARDING_NO}, the passes not
     * boarded, whose number is {@code null}, last, then by {@link BoardingPass_#TICKET_NO}).
     * <p>
     * The query, and its order, are derived from the method name, over the {@link BoardingPass_#FLIGHT_ID} part of
     * the id. The database sorts a {@code null} last on an ascending order, as the exercise's {@code NULLS LAST} says
     * outright. The exercise lists the matches whole, so there is no {@link Pageable}.
     *
     * @param flightId the value of the {@link BoardingPass_#FLIGHT_ID} attribute to match; must not be {@code null}.
     * @return a list of the boarding passes of {@code flightId}, by boarding number and then by ticket number; empty
     *         if there is none.
     */
    List<BoardingPass> findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(Integer flightId);

    /**
     * Selects the seat numbers with how many times each was boarded, the most first
     * ({@code DEMODB_BOARDING_PASSES_I_01}, which orders by {@code boarded} descending, then by {@code seatNo}; the
     * exercise's {@code LIMIT 5} is the first page of five).
     * <p>
     * The passes are grouped by {@link BoardingPass_#SEAT_NO} and counted; the count query counts the distinct seat
     * numbers.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the seat numbers with their counts, each a tuple of {@code seatNo} ({@link String}) and
     *         {@code boarded} ({@link Long}).
     */
    @Query(value = """
            SELECT p.seatNo AS seatNo, COUNT(p) AS boarded
            FROM BoardingPass p
            GROUP BY p.seatNo
            ORDER BY COUNT(p) DESC, p.seatNo""",
           countQuery = """
                   SELECT COUNT(DISTINCT p.seatNo)
                   FROM BoardingPass p""")
    Page<Tuple> selectBoardedCountsBySeatNo(Pageable pageable);
}
