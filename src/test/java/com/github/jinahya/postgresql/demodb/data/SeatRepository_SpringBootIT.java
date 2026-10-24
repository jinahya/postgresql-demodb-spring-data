package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import com.github.jinahya.postgresql.demodb.persistence.Seat_;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link SeatRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code seats_exercises_0N_*.sql} show, or counts taken again through the repository with a
 * {@link Specification}. The base's own tests, such as its {@code findById} round trips, run here too. Needs the
 * database of {@code application.yaml} up, with the demonstration database loaded; see that file.
 *
 * @see SeatRepository_DataJpaTest
 */
class SeatRepository_SpringBootIT
        extends _Repository_SpringBootIT<SeatRepository, Seat, SeatId> {

    /**
     * Creates a new instance.
     */
    SeatRepository_SpringBootIT() {
        super(SeatRepository.class, Seat.class, SeatId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Counts the seats of the specified airplane in the specified fare conditions, through a {@link Specification}.
     *
     * @param airplane       the airplane.
     * @param fareConditions the fare conditions, or {@code null} for all.
     * @return the number of seats.
     */
    private long countSeats(final AirplaneData airplane, final String fareConditions) {
        final Specification<Seat> ofAirplane = (r, q, b) -> b.equal(r.get(Seat_.airplaneData), airplane);
        if (fareConditions == null) {
            return repositoryInstance().count(ofAirplane);
        }
        return repositoryInstance().count(
                ofAirplane.and((r, q, b) -> b.equal(r.get(Seat_.fareConditions), fareConditions)));
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link SeatRepository#findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(String, String)}
     * ({@code DEMODB_SEATS_B_01}).
     */
    @Nested
    class FindAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc_Test {

        /**
         * Asserts that the exercise's airplane, the Airbus A320neo, {@code 32N}, and Business find the twenty-eight
         * seats the exercise's expected output shows, the first ten by seat number.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(
                    "32N", "Business");
            assertThat(found).hasSize(28);
            assertThat(found.subList(0, 10))
                    .extracting(Seat::getSeatNo)
                    .containsExactly("1A", "1C", "1D", "1F", "2A", "2C", "2D", "2F", "3A", "3C");
        }

        /**
         * Asserts that an airplane and a fare conditions find seats of that airplane in those conditions only,
         * sorted by seat number.
         *
         * @param fareConditions the fare conditions.
         */
        @ValueSource(strings = {"Economy", "Business"})
        @ParameterizedTest
        void __(final String fareConditions) {
            final var found = repositoryInstance().findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(
                    "32N", fareConditions);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(s -> {
                        assertThat(s.getAirplaneCode()).isEqualTo("32N");
                        assertThat(s.getFareConditions()).isEqualTo(fareConditions);
                    })
                    .isSortedAccordingTo(Comparator.comparing(Seat::getSeatNo));
        }

        /**
         * Asserts that a {@code null} argument is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            final var repository = repositoryInstance();
            assertThatThrownBy(() -> repository.findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(null, "Business"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository.findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc("32N", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link SeatRepository#selectSeatCountsByFareConditions(Pageable)} ({@code DEMODB_SEATS_I_01}).
     */
    @Nested
    class SelectSeatCountsByFareConditions_Test {

        /**
         * Asserts that the ten airplanes come with their seat counts, by code, as the exercise's expected output
         * shows, the two freighters with none.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectSeatCountsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("airplane", AirplaneData.class, AirplaneData::getAirplaneCode),
                                longColumn("economy"), longColumn("comfort"), longColumn("business"),
                                longColumn("seats"))
                    .containsExactly(
                            tuple("32N", 138L, 0L, 28L, 166L),
                            tuple("339", 224L, 28L, 29L, 281L),
                            tuple("351", 281L, 0L, 44L, 325L),
                            tuple("35X", 0L, 0L, 0L, 0L),
                            tuple("76F", 0L, 0L, 0L, 0L),
                            tuple("77W", 326L, 48L, 30L, 404L),
                            tuple("789", 188L, 21L, 48L, 257L),
                            tuple("7M7", 144L, 0L, 16L, 160L),
                            tuple("CR7", 52L, 12L, 6L, 70L),
                            tuple("E70", 72L, 0L, 6L, 78L));
            assertThat(page.getTotalElements()).isEqualTo(10L);
        }

        /**
         * Asserts that each airplane's counts are the ones a {@link Specification} counts, and that the three fare
         * conditions add up to all the seats.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectSeatCountsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .isNotEmpty()
                    .allSatisfy(t -> {
                        final var airplane = t.get("airplane", AirplaneData.class);
                        final var economy = longColumn("economy").apply(t);
                        final var comfort = longColumn("comfort").apply(t);
                        final var business = longColumn("business").apply(t);
                        assertThat(economy).isEqualTo(countSeats(airplane, "Economy"));
                        assertThat(comfort).isEqualTo(countSeats(airplane, "Comfort"));
                        assertThat(business).isEqualTo(countSeats(airplane, "Business"));
                        assertThat(longColumn("seats").apply(t))
                                .isEqualTo(economy + comfort + business)
                                .isEqualTo(countSeats(airplane, null));
                    });
        }

        /**
         * Asserts that pages of four walk the ten airplanes, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectSeatCountsByFareConditions(PageRequest.of(0, 4));
            assertThat(page0.getContent()).hasSize(4);
            assertThat(page0.getTotalElements()).isEqualTo(10L);
            final var page2 = repositoryInstance().selectSeatCountsByFareConditions(PageRequest.of(2, 4));
            assertThat(page2.getContent())
                    .extracting(column("airplane", AirplaneData.class, AirplaneData::getAirplaneCode))
                    .containsExactly("CR7", "E70");
            assertThat(page2.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectSeatCountsByFareConditions(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
