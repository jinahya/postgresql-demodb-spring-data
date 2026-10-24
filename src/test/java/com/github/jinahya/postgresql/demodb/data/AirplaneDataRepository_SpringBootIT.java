package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests {@link AirplaneDataRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code airplanes_data_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById}
 * round trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database
 * loaded; see that file.
 *
 * @see AirplaneDataRepository_DataJpaTest
 */
class AirplaneDataRepository_SpringBootIT
        extends _Repository_SpringBootIT<AirplaneDataRepository, AirplaneData, String> {

    /**
     * Creates a new instance.
     */
    AirplaneDataRepository_SpringBootIT() {
        super(AirplaneDataRepository.class, AirplaneData.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirplaneDataRepository#findAllByOrderByAirplaneCodeAsc()} ({@code DEMODB_AIRPLANES_DATA_B_01}).
     */
    @Nested
    class FindAllByOrderByAirplaneCodeAsc_Test {

        /**
         * Asserts that the ten airplanes of the sample data are found, by code, as the exercise's expected output
         * shows.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .extracting(AirplaneData::getAirplaneCode)
                    .containsExactly("32N", "339", "351", "35X", "76F", "77W", "789", "7M7", "CR7", "E70");
            assertThat(found.getFirst().getRange()).isEqualTo(6500);
            assertThat(found.getFirst().getSpeed()).isEqualTo(830);
        }

        /**
         * Asserts that every airplane is found, as many as the table holds, sorted by code.
         */
        @Test
        void __() {
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .hasSize((int) repositoryInstance().count())
                    .isSortedAccordingTo(Comparator.comparing(AirplaneData::getAirplaneCode));
        }
    }

    /**
     * Tests {@link AirplaneDataRepository#selectHoursAtFullRange(Pageable)} ({@code DEMODB_AIRPLANES_DATA_I_02}).
     */
    @Nested
    class SelectHoursAtFullRange_Test {

        /**
         * Rounds the specified hours to two places, as the exercise's SQL does.
         *
         * @param hours the hours.
         * @return the hours rounded.
         */
        private static BigDecimal rounded(final Number hours) {
            return BigDecimal.valueOf(hours.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }

        /**
         * Asserts that the ten airplanes, in one page, come with the hours the exercise's expected output shows, the
         * longest first.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectHoursAtFullRange(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("airplane", AirplaneData.class, AirplaneData::getAirplaneCode),
                                t -> rounded(t.get("hours", Number.class)))
                    .containsExactly(
                            tuple("351", new BigDecimal("18.29")),
                            tuple("77W", new BigDecimal("16.13")),
                            tuple("789", new BigDecimal("15.33")),
                            tuple("339", new BigDecimal("15.29")),
                            tuple("35X", new BigDecimal("9.63")),
                            tuple("7M7", new BigDecimal("8.33")),
                            tuple("32N", new BigDecimal("7.83")),
                            tuple("76F", new BigDecimal("7.06")),
                            tuple("E70", new BigDecimal("5.00")),
                            tuple("CR7", new BigDecimal("3.74")));
        }

        /**
         * Asserts that each airplane's hours are its range over its speed, and that the page is sorted by them, the
         * most first.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectHoursAtFullRange(Pageable.unpaged());
            assertThat(page.getContent())
                    .isNotEmpty()
                    .allSatisfy(t -> {
                        final var airplane = t.get("airplane", AirplaneData.class);
                        assertThat(t.get("hours", Number.class).doubleValue())
                                .isCloseTo((double) airplane.getRange() / airplane.getSpeed(), within(0.001));
                    })
                    .isSortedAccordingTo(
                            Comparator.comparing((Tuple t) -> t.get("hours", Number.class).doubleValue()).reversed());
        }

        /**
         * Asserts that pages of four walk the ten airplanes, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectHoursAtFullRange(PageRequest.of(0, 4));
            assertThat(page0.getContent()).hasSize(4);
            assertThat(page0.getTotalElements()).isEqualTo(10L);
            assertThat(page0.getTotalPages()).isEqualTo(3);
            final var page2 = repositoryInstance().selectHoursAtFullRange(PageRequest.of(2, 4));
            assertThat(page2.getContent())
                    .extracting(column("airplane", AirplaneData.class, AirplaneData::getAirplaneCode))
                    .containsExactly("E70", "CR7");
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectHoursAtFullRange(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
