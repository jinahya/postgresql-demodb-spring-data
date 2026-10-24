package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Booking;
import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link TicketRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the bookings and tickets it needs through the
 * fixtures, sets the passengers it is about, and the slice rolls them back when it ends. The booking's reference
 * is written by the {@code booking} association, and the inherited read-only attribute over the same column is
 * not loaded into a saved instance, so the tests assert through the association.
 *
 * @see TicketRepository_SpringBootIT
 */
class TicketRepository_DataJpaTest
        extends _Repository_DataJpaTest<TicketRepository, Ticket, String> {

    /**
     * Creates a new instance.
     */
    TicketRepository_DataJpaTest() {
        super(TicketRepository.class, Ticket.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns a passenger id no other test uses, within the twenty characters of the column.
     *
     * @return a random passenger id.
     */
    private static String randomPassengerId() {
        return UUID.randomUUID().toString().substring(0, 20);
    }

    /**
     * Saves a randomized ticket of the specified booking, for the specified passenger.
     *
     * @param booking       the booking.
     * @param passengerId   the passenger's id.
     * @param passengerName the passenger's name.
     * @return the saved ticket, managed in the test's persistence context.
     */
    private Ticket saveTicket(final Booking booking, final String passengerId, final String passengerName) {
        final var ticket = fixtures().ticket(booking);
        ticket.setPassengerId(passengerId);
        ticket.setPassengerName(passengerName);
        return ticket;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link TicketRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().ticket();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link TicketRepository#findAllByBookRefOrderByTicketNoAsc(String)} ({@code DEMODB_TICKETS_B_01}).
     */
    @Nested
    class FindAllByBookRefOrderByTicketNoAsc_Test {

        /**
         * Asserts that a booking with no ticket finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByBookRefOrderByTicketNoAsc(fixtures().booking().getBookRef());
            assertThat(found).isEmpty();
        }

        /**
         * Saves three tickets of a booking and one of another, and asserts that the booking's reference finds the
         * three, those very instances, by ticket number as the exercise sorts.
         */
        @Test
        void __() {
            final var booking = fixtures().booking();
            final var saved = Stream.of(fixtures().ticket(booking), fixtures().ticket(booking), fixtures().ticket(booking))
                    .sorted(Comparator.comparing(Ticket::getTicketNo))
                    .toList();
            final var other = fixtures().ticket();
            final var found = repositoryInstance().findAllByBookRefOrderByTicketNoAsc(booking.getBookRef());
            assertThat(found)
                    .containsExactlyElementsOf(saved)
                    .doesNotContain(other)
                    .allSatisfy(t -> assertThat(t.getBooking()).isSameAs(booking));
        }

        /**
         * Asserts that a {@code null} reference is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Saves three tickets of one passenger and one of another, and asserts that the two passengers are selected
         * with their counts, the most first, as the exercise sorts.
         */
        @Test
        void __() {
            final var booking = fixtures().booking();
            final var three = randomPassengerId();
            final var one = randomPassengerId();
            for (int i = 0; i < 3; i++) {
                saveTicket(booking, three, "Three");
            }
            saveTicket(booking, one, "One");
            final var page = repositoryInstance().selectTicketCountsByPassenger(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("passengerId", String.class), column("passengerName", String.class),
                                column("tickets", Long.class))
                    .containsExactly(tuple(three, "Three", 3L), tuple(one, "One", 1L));
        }

        /**
         * Saves a ticket of each of three passengers, and asserts that pages of two split them two and one, three in
         * all, which the count query says.
         */
        @Test
        void __Paged() {
            final var booking = fixtures().booking();
            for (int i = 0; i < 3; i++) {
                saveTicket(booking, randomPassengerId(), "Passenger " + i);
            }
            final var page0 = repositoryInstance().selectTicketCountsByPassenger(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectTicketCountsByPassenger(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
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
