package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests {@link AirplaneDataRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the airplanes it needs through the fixtures,
 * sets the attributes it is about, and the slice rolls them back when it ends, so a test starts over an empty table
 * and can count what it finds.
 *
 * @see AirplaneDataRepository_SpringBootIT
 */
class AirplaneDataRepository_DataJpaTest
        extends _Repository_DataJpaTest<AirplaneDataRepository, AirplaneData, String> {

    /**
     * Creates a new instance.
     */
    AirplaneDataRepository_DataJpaTest() {
        super(AirplaneDataRepository.class, AirplaneData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Saves a randomized airplane with the specified range and speed.
     *
     * @param range the range of the airplane.
     * @param speed the speed of the airplane.
     * @return the saved airplane, managed in the test's persistence context.
     */
    private AirplaneData saveAirplane(final int range, final int speed) {
        final var airplane = fixtures().airplaneData();
        airplane.setRange(range);
        airplane.setSpeed(speed);
        return airplane;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link AirplaneDataRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().airplaneData();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirplaneDataRepository#findAllByOrderByAirplaneCodeAsc()} ({@code DEMODB_AIRPLANES_DATA_B_01}).
     */
    @Nested
    class FindAllByOrderByAirplaneCodeAsc_Test {

        /**
         * Asserts that an empty table finds an empty list.
         */
        @Test
        void _Empty_NoAirplanes() {
            assertThat(repositoryInstance().findAllByOrderByAirplaneCodeAsc()).isEmpty();
        }

        /**
         * Saves three airplanes, and asserts that they are found, those very instances, by code, as the exercise
         * sorts.
         */
        @Test
        void __() {
            final var saved = Stream.of(fixtures().airplaneData(), fixtures().airplaneData(), fixtures().airplaneData())
                    .sorted(Comparator.comparing(AirplaneData::getAirplaneCode))
                    .toList();
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .containsExactlyElementsOf(saved)
                    .anySatisfy(a -> assertThat(a).isSameAs(saved.getFirst()));
        }
    }

    /**
     * Tests {@link AirplaneDataRepository#selectHoursAtFullRange(Pageable)} ({@code DEMODB_AIRPLANES_DATA_I_02}).
     */
    @Nested
    class SelectHoursAtFullRange_Test {

        /**
         * Saves airplanes flying ten, three and five hours at their full range, and asserts that they are selected
         * whole, with their hours, the longest first, as the exercise sorts.
         */
        @Test
        void __() {
            final var ten = saveAirplane(1000, 100);
            final var three = saveAirplane(900, 300);
            final var five = saveAirplane(500, 100);
            final var page = repositoryInstance().selectHoursAtFullRange(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("airplane", AirplaneData.class))
                    .containsExactly(ten, five, three);
            assertThat(page.getContent())
                    .extracting(t -> t.get("hours", Number.class).doubleValue())
                    .satisfiesExactly(
                            h -> assertThat(h).isCloseTo(10.0, within(0.001)),
                            h -> assertThat(h).isCloseTo(5.0, within(0.001)),
                            h -> assertThat(h).isCloseTo(3.0, within(0.001))
                    );
        }

        /**
         * Saves three airplanes, and asserts that pages of two split them two and one, three in all, which the count
         * query says.
         */
        @Test
        void __Paged() {
            saveAirplane(1000, 100);
            saveAirplane(900, 300);
            saveAirplane(500, 100);
            final var page0 = repositoryInstance().selectHoursAtFullRange(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectHoursAtFullRange(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectHoursAtFullRange(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
