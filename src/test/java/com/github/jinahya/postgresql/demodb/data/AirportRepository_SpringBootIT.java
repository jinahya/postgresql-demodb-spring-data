package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
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
 * Tests {@link AirportRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code airports_exercises_0N_*.sql} show, in English, the view's language for a session that sets
 * none. The base's own tests, such as its {@code findById} round trips, run here too. Needs the database of
 * {@code application.yaml} up, with the demonstration database loaded; see that file.
 *
 * @see AirportRepository_DataJpaTest
 */
class AirportRepository_SpringBootIT
        extends _Repository_SpringBootIT<AirportRepository, Airport, String> {

    /**
     * Creates a new instance.
     */
    AirportRepository_SpringBootIT() {
        super(AirportRepository.class, Airport.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirportRepository#findAllByCountryOrderByAirportCodeAsc(String)} ({@code DEMODB_AIRPORTS_B_01}).
     */
    @Nested
    class FindAllByCountryOrderByAirportCodeAsc_Test {

        /**
         * Asserts that the exercise's country, {@code Norway}, finds the fifty-five airports the exercise's expected
         * output shows, the first ten by code.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByCountryOrderByAirportCodeAsc("Norway");
            assertThat(found).hasSize(55);
            assertThat(found.subList(0, 10))
                    .extracting(Airport::getAirportCode, Airport::getAirportName, Airport::getCity)
                    .containsExactly(
                            tuple("AES", "Vigra", "Ålesund"),
                            tuple("ALF", "Alta", "Alta"),
                            tuple("ANX", "Andøya", "Andenes"),
                            tuple("BDU", "Bardufoss", "Bardufoss"),
                            tuple("BGO", "Flesland", "Bergen"),
                            tuple("BJF", "Båtsfjord", "Båtsfjord"),
                            tuple("BNN", "Brønnøysund", "Brønnøysund"),
                            tuple("BOO", "Bodø", "Bodø"),
                            tuple("BVG", "Berlevåg", "Berlevåg"),
                            tuple("DLD", "Dagali", "Geilo"));
            assertThat(found.subList(0, 10)).allSatisfy(a -> assertThat(a.getTimezone()).isEqualTo("Europe/Oslo"));
        }

        /**
         * Asserts that a country finds airports of that country only, sorted by code.
         *
         * @param country the country.
         */
        @ValueSource(strings = {"Norway", "Finland", "Japan"})
        @ParameterizedTest
        void __(final String country) {
            final var found = repositoryInstance().findAllByCountryOrderByAirportCodeAsc(country);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(a -> assertThat(a.getCountry()).isEqualTo(country))
                    .isSortedAccordingTo(Comparator.comparing(Airport::getAirportCode));
        }

        /**
         * Asserts that a {@code null} country is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullCountry() {
            assertThatThrownBy(() -> repositoryInstance().findAllByCountryOrderByAirportCodeAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link AirportRepository#selectAirportCountsByCountry(Pageable)} ({@code DEMODB_AIRPORTS_I_01}).
     */
    @Nested
    class SelectAirportCountsByCountry_Test {

        /**
         * Asserts that the first page of ten holds the ten countries with the most airports, with their counts, as
         * the exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectAirportCountsByCountry(PageRequest.of(0, 10));
            assertThat(page.getContent())
                    .extracting(column("country", String.class), column("airports", Long.class))
                    .containsExactly(
                            tuple("United States", 1129L),
                            tuple("Canada", 335L),
                            tuple("Australia", 218L),
                            tuple("China", 187L),
                            tuple("Brazil", 180L),
                            tuple("Russia", 158L),
                            tuple("France", 122L),
                            tuple("India", 110L),
                            tuple("Indonesia", 105L),
                            tuple("United Kingdom", 105L));
        }

        /**
         * Asserts that each country's count is the number of airports the derived query finds in it, and that the
         * page is sorted by the count, the most first, then by the country.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectAirportCountsByCountry(PageRequest.of(0, 20));
            assertThat(page.getContent())
                    .isNotEmpty()
                    .isSortedAccordingTo(Comparator.comparing(column("airports", Long.class)).reversed()
                                                 .thenComparing(column("country", String.class)))
                    .allSatisfy(t -> {
                        final var country = t.get("country", String.class);
                        assertThat((long) repositoryInstance().findAllByCountryOrderByAirportCodeAsc(country).size())
                                .as("airports of %s", country)
                                .isEqualTo(t.get("airports", Long.class));
                    });
        }

        /**
         * Asserts that the second page of ten goes on from the first, that the pages hold as many countries in all
         * as the count query says, and that the sum of every count is every airport.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectAirportCountsByCountry(PageRequest.of(0, 10));
            final var page1 = repositoryInstance().selectAirportCountsByCountry(page0.nextPageable());
            assertThat(page1.getContent())
                    .hasSize(10)
                    .allSatisfy(t -> assertThat(t.get("airports", Long.class)).isLessThanOrEqualTo(105L));
            final var all = repositoryInstance().selectAirportCountsByCountry(Pageable.unpaged());
            assertThat(page0.getTotalElements()).isEqualTo(all.getContent().size());
            assertThat(all.getContent().stream().mapToLong(column("airports", Long.class)::apply).sum())
                    .isEqualTo(repositoryInstance().count());
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectAirportCountsByCountry(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
