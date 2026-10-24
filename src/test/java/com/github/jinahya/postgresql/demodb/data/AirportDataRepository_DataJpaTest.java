package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link AirportDataRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the airports it needs through the fixtures,
 * sets the time zones it is about, random and unique to the test, and the slice rolls them back when it ends, so a
 * test starts over an empty table and can count what it finds.
 *
 * @see AirportDataRepository_SpringBootIT
 */
class AirportDataRepository_DataJpaTest
        extends _Repository_DataJpaTest<AirportDataRepository, AirportData, String> {

    /**
     * Creates a new instance.
     */
    AirportDataRepository_DataJpaTest() {
        super(AirportDataRepository.class, AirportData.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns a time zone name no other test uses.
     *
     * @return a random time zone name.
     */
    private static String randomTimezone() {
        return "Zone/" + UUID.randomUUID();
    }

    /**
     * Saves a randomized airport in the specified time zone.
     *
     * @param timezone the time zone of the airport.
     * @return the saved airport, managed in the test's persistence context.
     */
    private AirportData saveAirport(final String timezone) {
        final var airport = fixtures().airportData();
        airport.setTimezone(timezone);
        return airport;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link AirportDataRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().airportData();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirportDataRepository#findAllByTimezoneOrderByAirportCodeAsc(String)}
     * ({@code DEMODB_AIRPORTS_DATA_B_01}).
     */
    @Nested
    class FindAllByTimezoneOrderByAirportCodeAsc_Test {

        /**
         * Asserts that a time zone no airport is in finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            assertThat(repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc(randomTimezone())).isEmpty();
        }

        /**
         * Saves three airports in a time zone, and one in another, and asserts that the time zone finds the three,
         * those very instances, by code, as the exercise sorts.
         */
        @Test
        void __() {
            final var timezone = randomTimezone();
            final var saved = Stream.of(saveAirport(timezone), saveAirport(timezone), saveAirport(timezone))
                    .sorted(Comparator.comparing(AirportData::getAirportCode))
                    .toList();
            final var other = saveAirport(randomTimezone());
            final var found = repositoryInstance().findAllByTimezoneOrderByAirportCodeAsc(timezone);
            assertThat(found)
                    .containsExactlyElementsOf(saved)
                    .doesNotContain(other)
                    .allSatisfy(a -> assertThat(a.getTimezone()).isEqualTo(timezone));
            assertThat(found).anySatisfy(a -> assertThat(a).isSameAs(saved.getFirst()));
        }

        /**
         * Asserts that a {@code null} time zone is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Saves three airports in one time zone and one in another, and asserts that the two zones are selected
         * with their counts, the most first, as the exercise sorts.
         */
        @Test
        void __() {
            final var three = randomTimezone();
            final var one = randomTimezone();
            for (int i = 0; i < 3; i++) {
                saveAirport(three);
            }
            saveAirport(one);
            final var page = repositoryInstance().selectAirportCountsByTimezone(Pageable.unpaged());
            assertThat(page.getContent())
                    .extracting(column("timezone", String.class), column("airports", Long.class))
                    .containsExactly(tuple(three, 3L), tuple(one, 1L));
        }

        /**
         * Saves airports in three time zones, and asserts that pages of two split the zones two and one, three in
         * all, which the count query says.
         */
        @Test
        void __Paged() {
            for (int i = 0; i < 3; i++) {
                saveAirport(randomTimezone());
            }
            final var page0 = repositoryInstance().selectAirportCountsByTimezone(PageRequest.of(0, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().selectAirportCountsByTimezone(page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page1.hasNext()).isFalse();
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
