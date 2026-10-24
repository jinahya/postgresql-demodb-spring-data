package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link AirplaneRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code airplanes_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById} round
 * trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database loaded;
 * see that file.
 *
 * @see AirplaneRepository_DataJpaTest
 */
class AirplaneRepository_SpringBootIT
        extends _Repository_SpringBootIT<AirplaneRepository, Airplane, String> {

    /**
     * Creates a new instance.
     */
    AirplaneRepository_SpringBootIT() {
        super(AirplaneRepository.class, Airplane.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirplaneRepository#findAllByOrderByAirplaneCodeAsc()} ({@code DEMODB_AIRPLANES_B_01}).
     */
    @Nested
    class FindAllByOrderByAirplaneCodeAsc_Test {

        /**
         * Asserts that the ten airplanes of the view are found, by code, with their models in English, as the
         * exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .extracting(Airplane::getAirplaneCode, Airplane::getModel)
                    .containsExactly(
                            tuple("32N", "Airbus A320neo"),
                            tuple("339", "Airbus A330-900neo"),
                            tuple("351", "Airbus A350-1000"),
                            tuple("35X", "Airbus A350F"),
                            tuple("76F", "Boeing 767-300F"),
                            tuple("77W", "Boeing 777-300ER"),
                            tuple("789", "Boeing 787-9 Dreamliner"),
                            tuple("7M7", "Boeing 737 MAX 7"),
                            tuple("CR7", "Bombardier CRJ700"),
                            tuple("E70", "Embraer E170"));
        }

        /**
         * Asserts that every airplane is found, as many as the view holds, sorted by code.
         */
        @Test
        void __() {
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .hasSize((int) repositoryInstance().count())
                    .isSortedAccordingTo(Comparator.comparing(Airplane::getAirplaneCode));
        }
    }

    /**
     * Tests {@link AirplaneRepository#selectAirplaneCountsByMaker(Pageable)} ({@code DEMODB_AIRPLANES_I_01}).
     */
    @Nested
    class SelectAirplaneCountsByMaker_Test {

        /**
         * Asserts that the four makers come with their counts, the most airplanes first and then by name, as the
         * exercise's expected output shows, four in all.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectAirplaneCountsByMaker(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("maker", String.class), column("airplanes", Long.class))
                    .containsExactly(
                            tuple("Airbus", 4L),
                            tuple("Boeing", 4L),
                            tuple("Bombardier", 1L),
                            tuple("Embraer", 1L));
            assertThat(page.getTotalElements()).isEqualTo(4L);
        }

        /**
         * Asserts that each maker's count is the number of airplanes whose model starts with the maker's name, and
         * that the counts add up to every airplane.
         */
        @Test
        void __() {
            final var airplanes = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            final var page = repositoryInstance().selectAirplaneCountsByMaker(Pageable.unpaged());
            assertThat(page.getContent())
                    .isNotEmpty()
                    .allSatisfy(t -> {
                        final var maker = t.get("maker", String.class);
                        assertThat(airplanes.stream().filter(a -> a.getModel().startsWith(maker + ' ')).count())
                                .as("airplanes of %s", maker)
                                .isEqualTo(t.get("airplanes", Long.class));
                    });
            assertThat(page.getContent().stream().mapToLong(column("airplanes", Long.class)::apply).sum())
                    .isEqualTo(airplanes.size());
        }

        /**
         * Asserts that pages of three walk the four makers, as many in all as the count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectAirplaneCountsByMaker(PageRequest.of(0, 3));
            assertThat(page0.getContent()).hasSize(3);
            assertThat(page0.getTotalElements()).isEqualTo(4L);
            final var page1 = repositoryInstance().selectAirplaneCountsByMaker(page0.nextPageable());
            assertThat(page1.getContent())
                    .extracting(column("maker", String.class))
                    .containsExactly("Embraer");
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectAirplaneCountsByMaker(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
