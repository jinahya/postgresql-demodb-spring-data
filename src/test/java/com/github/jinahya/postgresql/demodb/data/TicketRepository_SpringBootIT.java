package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link TicketRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code tickets_exercises_0N_*.sql} show. The booking of a ticket is a lazy association, so a test
 * that reads it runs in a transaction. The base's own tests, such as its {@code findById} round trips, run here
 * too. Needs the database of {@code application.yaml} up, with the demonstration database loaded; see that file.
 *
 * @see TicketRepository_DataJpaTest
 */
class TicketRepository_SpringBootIT
        extends _Repository_SpringBootIT<TicketRepository, Ticket, String> {

    /**
     * Creates a new instance.
     */
    TicketRepository_SpringBootIT() {
        super(TicketRepository.class, Ticket.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link TicketRepository#findAllByBookRefOrderByTicketNoAsc(String)} ({@code DEMODB_TICKETS_B_01}).
     */
    @Nested
    class FindAllByBookRefOrderByTicketNoAsc_Test {

        /**
         * Asserts that the exercise's booking, {@code 0000EG}, finds its two tickets, by ticket number, as the
         * exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByBookRefOrderByTicketNoAsc("0000EG");
            assertThat(found)
                    .extracting(Ticket::getTicketNo, Ticket::getPassengerId, Ticket::getPassengerName,
                                Ticket::getOutbound)
                    .containsExactly(
                            tuple("0005434564147", "CA 1985281597520", "John Beland", true),
                            tuple("0005434564152", "CA 1985281597520", "John Beland", false));
        }

        /**
         * Asserts, in one transaction, that a booking's reference finds tickets of that booking only, sorted by
         * ticket number.
         *
         * @param bookRef the booking's reference.
         */
        @ValueSource(strings = {"0000EG", "6TAS39", "7UU4BE"})
        @ParameterizedTest
        @Transactional
        void __(final String bookRef) {
            final var found = repositoryInstance().findAllByBookRefOrderByTicketNoAsc(bookRef);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(t -> assertThat(t.getBooking().getBookRef()).isEqualTo(bookRef))
                    .isSortedAccordingTo(Comparator.comparing(Ticket::getTicketNo));
        }

        /**
         * Asserts that a {@code null} reference is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullBookRef() {
            assertThatThrownBy(() -> repositoryInstance().findAllByBookRefOrderByTicketNoAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link TicketRepository#selectTicketCountsByPassenger(Pageable)} ({@code DEMODB_TICKETS_I_01}).
     */
    @Nested
    class SelectTicketCountsByPassenger_Test {

        /**
         * Asserts that the first page of five holds the five passengers with the most tickets, with their counts,
         * as the exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectTicketCountsByPassenger(PageRequest.of(0, 5));
            assertThat(page.getContent())
                    .extracting(column("passengerId", String.class), column("passengerName", String.class),
                                column("tickets", Long.class))
                    .containsExactly(
                            tuple("CN 1303160118058", "Haiyun Yang", 5L),
                            tuple("CN 8458154433091", "Haiyun Zhao", 5L),
                            tuple("US 2995168528581", "William Thompson", 5L),
                            tuple("CA 0307652121299", "Pascal Lang", 4L),
                            tuple("CA 0314814108732", "Dawn Dupuis", 4L));
        }

        /**
         * Asserts that the page is sorted by the count, the most first, then by the passenger's id, and that each
         * count is at least the size of the pages that follow.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectTicketCountsByPassenger(PageRequest.of(0, 20));
            assertThat(page.getContent())
                    .hasSize(20)
                    .isSortedAccordingTo(Comparator.comparing(column("tickets", Long.class)).reversed()
                                                 .thenComparing(column("passengerId", String.class)))
                    .allSatisfy(t -> assertThat(t.get("tickets", Long.class)).isBetween(1L, 5L));
        }

        /**
         * Asserts that the second page of five goes on from the first, with counts no higher, and that there are as
         * many passengers in all as the count query says, at most every ticket.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectTicketCountsByPassenger(PageRequest.of(0, 5));
            final var page1 = repositoryInstance().selectTicketCountsByPassenger(page0.nextPageable());
            assertThat(page1.getContent())
                    .hasSize(5)
                    .allSatisfy(t -> assertThat(t.get("tickets", Long.class)).isLessThanOrEqualTo(4L));
            assertThat(page0.getTotalElements())
                    .isPositive()
                    .isLessThanOrEqualTo(repositoryInstance().count());
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectTicketCountsByPassenger(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
