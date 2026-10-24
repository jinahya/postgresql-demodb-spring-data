package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.BoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.BoardingPassId;
import com.github.jinahya.postgresql.demodb.persistence.Flight;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link BoardingPassRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the flights, tickets, segments and passes it
 * needs through the fixtures, sets the seat and boarding numbers it is about, and the slice rolls them back when it
 * ends. A pass's id is its segment's, so what a save returns is the managed copy, which the tests keep.
 *
 * @see BoardingPassRepository_SpringBootIT
 */
class BoardingPassRepository_DataJpaTest
        extends _Repository_DataJpaTest<BoardingPassRepository, BoardingPass, BoardingPassId> {

    /**
     * Creates a new instance.
     */
    BoardingPassRepository_DataJpaTest() {
        super(BoardingPassRepository.class, BoardingPass.class, BoardingPassId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a boarding pass of a new ticket on the specified flight, for the specified seat, with the specified
     * boarding number.
     *
     * @param flight     the flight.
     * @param seatNo     the seat number.
     * @param boardingNo the boarding number; {@code null} for a pass not boarded.
     * @return the saved pass, managed in the test's persistence context.
     */
    private BoardingPass savePass(final Flight flight, final String seatNo, final Integer boardingNo) {
        final var pass = fixtures().boardingPass(fixtures().segment(fixtures().ticket(), flight));
        pass.setSeatNo(seatNo);
        pass.setBoardingNo(boardingNo);
        return pass;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link BoardingPassRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().boardingPass();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link BoardingPassRepository#findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(Integer)}
     * ({@code DEMODB_BOARDING_PASSES_B_01}).
     */
    @Nested
    class FindAllByFlightIdOrderByBoardingNoAscTicketNoAsc_Test {

        /**
         * Asserts that a flight with no pass finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(
                    fixtures().flight().getFlightId());
            assertThat(found).isEmpty();
        }

        /**
         * Saves three passes of a flight, the second to board first, then one not boarded, then the first to board,
         * and a pass of another flight, and asserts that the flight finds its three, in boarding order as the
         * exercise sorts, the one not boarded last.
         */
        @Test
        void __() {
            final var flight = fixtures().flight();
            final var second = savePass(flight, "2A", 2);
            final var notBoarded = savePass(flight, "3A", null);
            final var first = savePass(flight, "1A", 1);
            final var other = savePass(fixtures().flight(), "1A", 1);
            final var found = repositoryInstance().findAllByFlightIdOrderByBoardingNoAscTicketNoAsc(
                    flight.getFlightId());
            assertThat(found)
                    .containsExactly(first, second, notBoarded)
                    .doesNotContain(other)
                    .allSatisfy(p -> assertThat(p.getFlightId()).isEqualTo(flight.getFlightId()));
        }

        /**
         * Asserts that a {@code null} flight id is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Saves two passes for one seat number, on two flights, and one for another, and asserts that the two seat
         * numbers are selected with their counts, the most first, as the exercise sorts.
         */
        @Test
        void __() {
            savePass(fixtures().flight(), "2B", 1);
            savePass(fixtures().flight(), "1A", 1);
            savePass(fixtures().flight(), "1A", 1);
            final var page = repositoryInstance().selectBoardedCountsBySeatNo(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("seatNo", String.class), column("boarded", Long.class))
                    .containsExactly(tuple("1A", 2L), tuple("2B", 1L));
        }

        /**
         * Saves a pass for each of three seat numbers, and asserts that pages of two split them two and one, three in
         * all, which the count query says.
         */
        @Test
        void __Paged() {
            final var flight = fixtures().flight();
            for (int i = 1; i <= 3; i++) {
                savePass(flight, i + "A", i);
            }
            final var page0 = repositoryInstance().selectBoardedCountsBySeatNo(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectBoardedCountsBySeatNo(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
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
