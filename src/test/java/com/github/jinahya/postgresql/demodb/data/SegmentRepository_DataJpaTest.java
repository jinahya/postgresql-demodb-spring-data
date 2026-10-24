package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
import com.github.jinahya.postgresql.demodb.persistence.Ticket;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests {@link SegmentRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the tickets, flights and segments it needs
 * through the fixtures, sets the fare conditions and the prices it is about, and the slice rolls them back when it
 * ends. A segment's id is derived from its ticket and its flight, so what a save returns is the managed copy, which
 * the tests keep; the flights' ids ascend in the order they are saved, which is what the exercise orders by.
 *
 * @see SegmentRepository_SpringBootIT
 */
class SegmentRepository_DataJpaTest
        extends _Repository_DataJpaTest<SegmentRepository, Segment, SegmentId> {

    /**
     * Creates a new instance.
     */
    SegmentRepository_DataJpaTest() {
        super(SegmentRepository.class, Segment.class, SegmentId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a segment of the specified ticket on the specified flight, in the specified fare conditions, for the
     * specified price.
     *
     * @param ticket         the ticket.
     * @param flight         the flight.
     * @param fareConditions the fare conditions.
     * @param price          the price.
     * @return the saved segment, managed in the test's persistence context.
     */
    private Segment saveSegment(final Ticket ticket, final Flight flight, final String fareConditions,
                                final String price) {
        final var segment = fixtures().segment(ticket, flight);
        segment.setFareConditions(fareConditions);
        segment.setPrice(new BigDecimal(price));
        return segment;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link SegmentRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().segment();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link SegmentRepository#findAllByTicketNoOrderByFlightIdAsc(String)} ({@code DEMODB_SEGMENTS_B_01}).
     */
    @Nested
    class FindAllByTicketNoOrderByFlightIdAsc_Test {

        /**
         * Asserts that a ticket with no segment finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByTicketNoOrderByFlightIdAsc(fixtures().ticket().getTicketNo());
            assertThat(found).isEmpty();
        }

        /**
         * Saves two segments of a ticket, on a later flight first, and a segment of another ticket, and asserts that
         * the ticket's number finds its two, those very instances, by flight id as the exercise sorts.
         */
        @Test
        void __() {
            final var ticket = fixtures().ticket();
            final var earlier = fixtures().flight();
            final var later = fixtures().flight();
            final var onLater = fixtures().segment(ticket, later);
            final var onEarlier = fixtures().segment(ticket, earlier);
            final var other = fixtures().segment(fixtures().ticket(), earlier);
            final var found = repositoryInstance().findAllByTicketNoOrderByFlightIdAsc(ticket.getTicketNo());
            assertThat(found)
                    .containsExactly(onEarlier, onLater)
                    .doesNotContain(other)
                    .allSatisfy(s -> assertThat(s.getTicket()).isSameAs(ticket));
        }

        /**
         * Asserts that a {@code null} ticket number is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullTicketNo() {
            assertThatThrownBy(() -> repositoryInstance().findAllByTicketNoOrderByFlightIdAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link SegmentRepository#selectPriceStatisticsByFareConditions(Pageable)}
     * ({@code DEMODB_SEGMENTS_I_01}).
     */
    @Nested
    class SelectPriceStatisticsByFareConditions_Test {

        /**
         * Saves two Economy segments and a Business one, and asserts that the two fare conditions are selected, by
         * name as the exercise sorts, with the cheapest, the average and the dearest price of each.
         */
        @Test
        void __() {
            final var flight = fixtures().flight();
            saveSegment(fixtures().ticket(), flight, "Economy", "100.00");
            saveSegment(fixtures().ticket(), flight, "Economy", "300.00");
            saveSegment(fixtures().ticket(), flight, "Business", "500.00");
            final var page = repositoryInstance().selectPriceStatisticsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("fareConditions", String.class))
                    .containsExactly("Business", "Economy");
            assertThat(page.getContent())
                    .satisfiesExactly(
                            t -> {
                                assertThat(t.get("cheapest", BigDecimal.class)).isEqualByComparingTo("500.00");
                                assertThat(t.get("average", Number.class).doubleValue()).isCloseTo(500.0, within(0.001));
                                assertThat(t.get("dearest", BigDecimal.class)).isEqualByComparingTo("500.00");
                            },
                            t -> {
                                assertThat(t.get("cheapest", BigDecimal.class)).isEqualByComparingTo("100.00");
                                assertThat(t.get("average", Number.class).doubleValue()).isCloseTo(200.0, within(0.001));
                                assertThat(t.get("dearest", BigDecimal.class)).isEqualByComparingTo("300.00");
                            });
        }

        /**
         * Saves a segment in each of the three fare conditions, and asserts that pages of two split them two and
         * one, three in all, which the count query says.
         */
        @Test
        void __Paged() {
            final var flight = fixtures().flight();
            for (final var fareConditions : new String[] {"Economy", "Comfort", "Business"}) {
                saveSegment(fixtures().ticket(), flight, fareConditions, "100.00");
            }
            final var page0 = repositoryInstance().selectPriceStatisticsByFareConditions(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectPriceStatisticsByFareConditions(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectPriceStatisticsByFareConditions(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
