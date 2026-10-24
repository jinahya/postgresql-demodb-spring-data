package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import com.github.jinahya.postgresql.demodb.persistence.Ticket_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link Ticket}, the {@code tickets} table of the demonstration database.
 * <p>
 * The id is the ticket number, a {@link String} assigned by whoever creates the row. The booking a ticket belongs to
 * is the {@code booking} association; the same column is the read-only {@link Ticket_#BOOK_REF} attribute, which the
 * derived query below names. The methods below answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code tickets} table, one method per exercise, named in each method's javadoc by the exercise's id
 * ({@code DEMODB_TICKETS_B_01}, ...). A Basic exercise is a derived query, with the order the exercise asks for in
 * its name, and returns a {@link List}, with no {@link Pageable}. An Intermediate one takes a {@link Pageable}, the
 * page, and returns a {@link Page}; the order the exercise asks for is in the query, and its {@code LIMIT} is the
 * size of the caller's page. A result that is not an entity comes back as a {@link jakarta.persistence.Tuple}, each
 * element read by its alias, with its type.
 *
 * @see Ticket
 * @see Ticket_
 */
@Repository
public interface TicketRepository
        extends _Repository<Ticket, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the tickets of the specified booking, by {@link Ticket_#TICKET_NO} ({@code DEMODB_TICKETS_B_01}, which
     * asks for the tickets of the booking {@code 0000EG}, ordered by {@link Ticket_#TICKET_NO}).
     * <p>
     * The query, and its order, are derived from the method name, over the read-only {@link Ticket_#BOOK_REF}
     * attribute, the booking's reference. The exercise lists the matches whole, so there is no {@link Pageable}.
     *
     * @param bookRef the value of the {@link Ticket_#BOOK_REF} attribute to match; must not be {@code null}.
     * @return a list of the tickets of {@code bookRef}, by ticket number; empty if there is none.
     */
    List<Ticket> findAllByBookRefOrderByTicketNoAsc(String bookRef);

    /**
     * Selects the passengers with how many tickets each holds, the most first ({@code DEMODB_TICKETS_I_01}, which
     * orders by {@code tickets} descending, then by {@code passengerId}; the exercise's {@code LIMIT 5} is the first
     * page of five).
     * <p>
     * The tickets are grouped by the passenger's id and name, and counted. The count query counts the distinct
     * passenger ids, a passenger id having one name.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the passengers with their numbers of tickets, each a tuple of {@code passengerId} and
     *         {@code passengerName} ({@link String}s) and {@code tickets} ({@link Long}).
     */
    @Query(value = """
            SELECT t.passengerId AS passengerId, t.passengerName AS passengerName, COUNT(t) AS tickets
            FROM Ticket t
            GROUP BY t.passengerId, t.passengerName
            ORDER BY COUNT(t) DESC, t.passengerId""",
           countQuery = """
                   SELECT COUNT(DISTINCT t.passengerId)
                   FROM Ticket t""")
    Page<Tuple> selectTicketCountsByPassenger(Pageable pageable);
}
