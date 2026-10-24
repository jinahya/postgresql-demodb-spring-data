package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.Seat;
import com.github.jinahya.postgresql.demodb.persistence.SeatId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link SeatRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the airplanes and seats it needs, through
 * the fixtures and the repository under test, with the seat numbers and the fare conditions it is about, and the
 * slice rolls them back when it ends. A seat's id is derived from its airplane and its number, so what a save
 * returns is the managed copy, which the tests keep.
 *
 * @see SeatRepository_SpringBootIT
 */
class SeatRepository_DataJpaTest
        extends _Repository_DataJpaTest<SeatRepository, Seat, SeatId> {

    /**
     * Creates a new instance.
     */
    SeatRepository_DataJpaTest() {
        super(SeatRepository.class, Seat.class, SeatId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a seat of the specified airplane, with the specified number and fare conditions.
     *
     * @param airplane       the airplane.
     * @param seatNo         the seat number.
     * @param fareConditions the fare conditions.
     * @return the saved seat, managed in the test's persistence context.
     */
    private Seat saveSeat(final AirplaneData airplane, final String seatNo, final String fareConditions) {
        final var seat = Seat.of(airplane, seatNo);
        seat.setFareConditions(fareConditions);
        return repositoryInstance().save(seat);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link SeatRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().seat();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link SeatRepository#findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(String, String)}
     * ({@code DEMODB_SEATS_B_01}).
     */
    @Nested
    class FindAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc_Test {

        /**
         * Asserts that an airplane with no seat finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(
                    fixtures().airplaneData().getAirplaneCode(), "Business");
            assertThat(found).isEmpty();
        }

        /**
         * Saves three Business seats and an Economy seat of an airplane, and a Business seat of another, and asserts
         * that the airplane and Business find the three, by seat number as text, as the exercise sorts.
         */
        @Test
        void __() {
            final var airplane = fixtures().airplaneData();
            final var seat2a = saveSeat(airplane, "2A", "Business");
            final var seat1a = saveSeat(airplane, "1A", "Business");
            final var seat10a = saveSeat(airplane, "10A", "Business");
            final var economy = saveSeat(airplane, "3A", "Economy");
            final var other = saveSeat(fixtures().airplaneData(), "1A", "Business");
            final var found = repositoryInstance().findAllByAirplaneCodeAndFareConditionsOrderBySeatNoAsc(
                    airplane.getAirplaneCode(), "Business");
            assertThat(found)
                    .containsExactly(seat10a, seat1a, seat2a)
                    .doesNotContain(economy, other);
        }

        /**
         * Asserts that a {@code null} argument is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Saves an airplane with two Economy seats and a Business seat, and an airplane with none, and asserts that
         * both are selected whole, by code as the exercise sorts, with their counts, the airplane with no seat as a
         * row of zeros.
         */
        @Test
        void __() {
            final var seated = fixtures().airplaneData();
            saveSeat(seated, "1A", "Economy");
            saveSeat(seated, "1B", "Economy");
            saveSeat(seated, "2A", "Business");
            final var empty = fixtures().airplaneData();
            final var expected = Stream.of(seated, empty)
                    .sorted(Comparator.comparing(AirplaneData::getAirplaneCode))
                    .map(a -> a == seated ? tuple(seated, 2L, 0L, 1L, 3L) : tuple(empty, 0L, 0L, 0L, 0L))
                    .toList();
            final var page = repositoryInstance().selectSeatCountsByFareConditions(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("airplane", AirplaneData.class), longColumn("economy"), longColumn("comfort"),
                                longColumn("business"), longColumn("seats"))
                    .containsExactlyElementsOf(expected);
        }

        /**
         * Saves three airplanes, and asserts that pages of two split them two and one, three in all, which the count
         * query says.
         */
        @Test
        void __Paged() {
            for (int i = 0; i < 3; i++) {
                fixtures().seat();
            }
            final var page0 = repositoryInstance().selectSeatCountsByFareConditions(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectSeatCountsByFareConditions(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
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
