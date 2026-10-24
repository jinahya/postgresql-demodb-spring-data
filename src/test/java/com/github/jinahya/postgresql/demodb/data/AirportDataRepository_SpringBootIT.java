package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
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
 * Tests {@link AirportDataRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code airports_data_exercises_0N_*.sql} show. The base's own tests, such as its {@code findById}
 * round trips, run here too. Needs the database of {@code application.yaml} up, with the demonstration database
 * loaded; see that file.
 *
 * @see AirportDataRepository_DataJpaTest
 */
class AirportDataRepository_SpringBootIT
        extends _Repository_SpringBootIT<AirportDataRepository, AirportData, String> {

    /**
     * Creates a new instance.
     */
    AirportDataRepository_SpringBootIT() {
        super(AirportDataRepository.class, AirportData.class, String.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirportDataRepository#findAllByTimezoneOrderByAirportCodeAsc(String)}
     * ({@code DEMODB_AIRPORTS_DATA_B_01}).
     */
    @Nested
    class FindAllByTimezoneOrderByAirportCodeAsc_Test {

        /**
         * Asserts that the exercise's time zone, {@code Europe/Moscow}, finds the sixty-five airports the exercise's
         * expected output shows, the first ten by code.
         */
        @Test
        void __Exercise() {
            final var found = repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc("Europe/Moscow");
            assertThat(found).hasSize(65);
            assertThat(found.subList(0, 10))
                    .extracting(AirportData::getAirportCode)
                    .containsExactly("AAQ", "AER", "AMV", "ARH", "BKA", "BWO", "BZK", "CEE", "CKL", "CSH");
            assertThat(found.getFirst().getAirportName()).contains("\"en\": \"Vityazevo\"");
        }

        /**
         * Asserts that a time zone finds airports in that zone only, sorted by code.
         *
         * @param timezone the time zone.
         */
        @ValueSource(strings = {"Europe/Moscow", "Asia/Seoul", "America/New_York"})
        @ParameterizedTest
        void __(final String timezone) {
            final var found = repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc(timezone);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(a -> assertThat(a.getTimezone()).isEqualTo(timezone))
                    .isSortedAccordingTo(Comparator.comparing(AirportData::getAirportCode));
        }

        /**
         * Asserts that a {@code null} time zone is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullTimezone() {
            assertThatThrownBy(() -> repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link AirportDataRepository#selectAirportCountsByTimezone(Pageable)}
     * ({@code DEMODB_AIRPORTS_DATA_I_02}).
     */
    @Nested
    class SelectAirportCountsByTimezone_Test {

        /**
         * Asserts that the first page of ten holds the ten time zones with the most airports, with their counts, as
         * the exercise's expected output shows.
         */
        @Test
        void __Exercise() {
            final var page = repositoryInstance().selectAirportCountsByTimezone(PageRequest.of(0, 10));
            assertThat(page.getContent())
                    .extracting(column("timezone", String.class), column("airports", Long.class))
                    .containsExactly(
                            tuple("America/New_York", 366L),
                            tuple("America/Chicago", 296L),
                            tuple("Asia/Shanghai", 188L),
                            tuple("America/Anchorage", 168L),
                            tuple("America/Los_Angeles", 143L),
                            tuple("Europe/Paris", 123L),
                            tuple("America/Toronto", 120L),
                            tuple("America/Denver", 110L),
                            tuple("Asia/Kolkata", 110L),
                            tuple("Europe/London", 106L));
        }

        /**
         * Asserts that each time zone's count is the number of airports the derived query finds in it, and that the
         * page is sorted by the count, the most first, then by the zone.
         */
        @Test
        void __() {
            final var page = repositoryInstance().selectAirportCountsByTimezone(PageRequest.of(0, 20));
            assertThat(page.getContent())
                    .isNotEmpty()
                    .isSortedAccordingTo(Comparator.comparing(column("airports", Long.class)).reversed()
                                                 .thenComparing(column("timezone", String.class)))
                    .allSatisfy(t -> {
                        final var timezone = t.get("timezone", String.class);
                        assertThat((long) repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc(timezone).size())
                                .as("airports in %s", timezone)
                                .isEqualTo(t.get("airports", Long.class));
                    });
        }

        /**
         * Asserts that the second page of ten goes on from the first, that the pages hold as many time zones in all
         * as the count query says, and that the sum of every count is every airport.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().selectAirportCountsByTimezone(PageRequest.of(0, 10));
            final var page1 = repositoryInstance().selectAirportCountsByTimezone(page0.nextPageable());
            assertThat(page1.getContent())
                    .hasSize(10)
                    .allSatisfy(t -> assertThat(t.get("airports", Long.class)).isLessThanOrEqualTo(106L));
            final var all = repositoryInstance().selectAirportCountsByTimezone(Pageable.unpaged());
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
            assertThatThrownBy(() -> repositoryInstance().selectAirportCountsByTimezone(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
