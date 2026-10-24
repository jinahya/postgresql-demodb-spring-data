package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Segment;
import com.github.jinahya.postgresql.demodb.persistence.SegmentId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests {@link SegmentRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code segments_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById} round
 * trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database loaded;
 * see that file.
 *
 * @see SegmentRepository_DataJpaTest
 */
class SegmentRepository_SpringBootIT
        extends _Repository_SpringBootIT<SegmentRepository, Segment, SegmentId> {

    /**
     * Creates a new instance.
     */
    SegmentRepository_SpringBootIT() {
        super(SegmentRepository.class, Segment.class, SegmentId.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link SegmentRepository#findAllByTicketNoOrderByFlightIdAsc(String)} ({@code DEMODB_SEGMENTS_B_01}).
     */
    @Nested
    class FindAllByTicketNoOrderByFlightIdAsc_Test {

        /**
         * Asserts that the exercise's ticket, {@code 0005432000000}, finds its one segment, as the exercise's expected
         * output shows.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByTicketNoOrderByFlightIdAsc("0005432000000");
            assertThat(found)
                    .extracting(Segment::getFlightId, Segment::getFareConditions)
                    .containsExactly(tuple(108, "Economy"));
            assertThat(found.getFirst().getPrice()).isEqualByComparingTo("3250.00");
        }

        /**
         * Asserts that a ticket number finds segments of that ticket only, sorted by flight id.
         *
         * @param ticketNo the ticket number.
         */
        @ValueSource(strings = {"0005432000000", "0005434564147", "0005434564152"})
        @ParameterizedTest
        void __(final String ticketNo) {
            final var found = repositoryInstance().findAllByTicketNoOrderByFlightIdAsc(ticketNo);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(s -> assertThat(s.getTicketNo()).isEqualTo(ticketNo))
                    .isSortedAccordingTo(Comparator.comparing(Segment::getFlightId));
        }

        /**
         * Asserts that a {@code null} ticket number is rejected before any query runs.
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
         * Asserts that the three fare conditions come with their prices, by name, as the exercise's expected output
         * shows, the average within a cent of the exercise's rounded one, three in all.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectPriceStatisticsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("fareConditions", String.class))
                    .containsExactly("Business", "Comfort", "Economy");
            assertThat(page.getContent())
                    .satisfiesExactly(
                            t -> {
                                assertThat(t.get("cheapest", BigDecimal.class)).isEqualByComparingTo("3500.00");
                                assertThat(t.get("average", Number.class).doubleValue()).isCloseTo(11826.86, within(0.005));
                                assertThat(t.get("dearest", BigDecimal.class)).isEqualByComparingTo("119500.00");
                            },
                            t -> {
                                assertThat(t.get("cheapest", BigDecimal.class)).isEqualByComparingTo("2275.00");
                                assertThat(t.get("average", Number.class).doubleValue()).isCloseTo(6422.43, within(0.005));
                                assertThat(t.get("dearest", BigDecimal.class)).isEqualByComparingTo("65325.00");
                            },
                            t -> {
                                assertThat(t.get("cheapest", BigDecimal.class)).isEqualByComparingTo("1750.00");
                                assertThat(t.get("average", Number.class).doubleValue()).isCloseTo(5492.63, within(0.005));
                                assertThat(t.get("dearest", BigDecimal.class)).isEqualByComparingTo("59750.00");
                            });
            assertThat(page.getTotalElements()).isEqualTo(3L);
        }

        /**
         * Asserts that, for each fare conditions, the cheapest price is at most the average, which is at most the
         * dearest.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectPriceStatisticsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .isNotEmpty()
                    .allSatisfy(t -> {
                        final var average = t.get("average", Number.class).doubleValue();
                        assertThat(t.get("cheapest", BigDecimal.class).doubleValue()).isLessThanOrEqualTo(average);
                        assertThat(t.get("dearest", BigDecimal.class).doubleValue()).isGreaterThanOrEqualTo(average);
                    });
        }

        /**
         * Asserts that pages of two walk the three fare conditions, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectPriceStatisticsByFareConditions(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectPriceStatisticsByFareConditions(page0.nextPageable());
            assertThat(page1.getContent())
                    .extracting(column("fareConditions", String.class))
                    .containsExactly("Economy");
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
