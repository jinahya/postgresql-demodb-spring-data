package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link BoardingPassRepository} against the demonstration database, through
 * {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code boarding_passes_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById}
 * round trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database
 * loaded; see that file.
 *
 * @see BoardingPassRepository_DataJpaTest
 */
class BoardingPassRepository_SpringBootIT
        extends _Repository_SpringBootIT<BoardingPassRepository, BoardingPass, BoardingPassId> {

    /**
     * Creates a new instance.
     */
    BoardingPassRepository_SpringBootIT() {
        super(BoardingPassRepository.class, BoardingPass.class, BoardingPassId.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link BoardingPassRepository#findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(Integer)}
     * ({@code DEMODB_BOARDING_PASSES_B_01}).
     */
    @Nested
    class FindAllByFlightIdOrderByBoardingNoAscTicketNoAsc_Test {

        /**
         * Asserts that the exercise's flight, {@code 1}, finds the forty-eight passes the exercise's expected output
         * shows, the first five in boarding order.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(1);
            assertThat(found).hasSize(48);
            assertThat(found.subList(0, 5))
                    .extracting(BoardingPass::getTicketNo, BoardingPass::getSeatNo, BoardingPass::getBoardingNo)
                    .containsExactly(
                            tuple("0005432486526", "24A", 1),
                            tuple("0005432803911", "13C", 2),
                            tuple("0005432498784", "14B", 3),
                            tuple("0005432803924", "20A", 4),
                            tuple("0005432533210", "29D", 5));
        }

        /**
         * Asserts that a flight finds passes of that flight only, in boarding order, the passes not boarded last,
         * then by ticket number.
         *
         * @param flightId the flight's id.
         */
        @ValueSource(ints = {1, 2, 108})
        @ParameterizedTest
        void __(final int flightId) {
            final var found = repositoryInstance().findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(flightId);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(p -> assertThat(p.getFlightId()).isEqualTo(flightId))
                    .isSortedAccordingTo(
                            Comparator.comparing(BoardingPass::getBoardingNo, Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(BoardingPass::getTicketNo));
        }

        /**
         * Asserts that a {@code null} flight id is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullFlightId() {
            assertThatThrownBy(() -> repositoryInstance().findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link BoardingPassRepository#selectBoardedCountsBySeatNo(Pageable)}
     * ({@code DEMODB_BOARDING_PASSES_I_01}).
     */
    @Nested
    class SelectBoardedCountsBySeatNo_Test {

        /**
         * Asserts that the first page of five holds the five seat numbers boarded the most, with their counts, as
         * the exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectBoardedCountsBySeatNo(PageRequest.of(0, 5));
            assertThat(page.getContent())
                    .extracting(column("seatNo", String.class), column("boarded", Long.class))
                    .containsExactly(
                            tuple("20D", 9773L),
                            tuple("22D", 9752L),
                            tuple("20A", 9742L),
                            tuple("20C", 9732L),
                            tuple("22A", 9730L));
        }

        /**
         * Asserts that the page is sorted by the count, the most first, then by the seat number, and that the counts
         * of every seat number add up to every pass.
         */
        @Test
        void __() {
            final var all = repositoryInstance().selectBoardedCountsBySeatNo(Pageable.unpaged());
            assertThat(all.getContent())
                    .isNotEmpty()
                    .isSortedAccordingTo(Comparator.comparing(column("boarded", Long.class)).reversed()
                                                 .thenComparing(column("seatNo", String.class)));
            assertThat(all.getContent().stream().mapToLong(column("boarded", Long.class)::apply).sum())
                    .isEqualTo(repositoryInstance().count());
        }

        /**
         * Asserts that the second page of five goes on from the first, with counts no higher, and that there are as
         * many seat numbers in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectBoardedCountsBySeatNo(PageRequest.of(0, 5));
            final var page1 = repositoryInstance().selectBoardedCountsBySeatNo(page0.nextPageable());
            assertThat(page1.getContent())
                    .hasSize(5)
                    .allSatisfy(t -> assertThat(t.get("boarded", Long.class)).isLessThanOrEqualTo(9730L));
            final var all = repositoryInstance().selectBoardedCountsBySeatNo(Pageable.unpaged());
            assertThat(page0.getTotalElements()).isEqualTo(all.getContent().size());
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectBoardedCountsBySeatNo(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
