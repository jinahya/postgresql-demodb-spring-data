package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import com.github.jinahya.postgresql.demodb.persistence.Route_;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests {@link RouteRepository} against the demonstration database, through {@link _Repository_SpringBootIT}.
 * <p>
 * See that class for the context and the transactions. The tests here only read: the expected rows are the ones
 * upstream's {@code routes_exercises_0N_*.sql} show. The airports and the airplane of a route are lazy
 * associations, so a test that reads them runs in a transaction. The base's own tests, such as its
 * {@code findById} round trips, run here too. Needs the database of {@code application.yaml} up, with the
 * demonstration database loaded; see that file.
 *
 * @see RouteRepository_DataJpaTest
 */
class RouteRepository_SpringBootIT
        extends _Repository_SpringBootIT<RouteRepository, Route, RouteId> {

    /**
     * A sort by validity; by the attribute's name, since the metamodel's {@code Route_.validity} is filled in only
     * once the context is up, after this class is loaded.
     */
    private static final Sort BY_VALIDITY = Sort.by(Sort.Direction.ASC, Route_.VALIDITY);

    /**
     * Creates a new instance.
     */
    RouteRepository_SpringBootIT() {
        super(RouteRepository.class, Route.class, RouteId.class);
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link RouteRepository#findAllByDepartureAirportOrderByRouteNoAscValidityAsc(String)}
     * ({@code DEMODB_ROUTES_B_01}).
     */
    @Nested
    class FindAllByDepartureAirportOrderByRouteNoAscValidityAsc_Test {

        /**
         * Asserts that the exercise's airport, Sheremetyevo, {@code SVO}, finds the thirty-four routes the exercise's
         * expected output shows, the first ten by number and then by validity, each from {@code SVO}.
         */
        @Test
        @Transactional
        void __Exercise() {
            final var found = repositoryInstance().findAllByDepartureAirportOrderByRouteNoAscValidityAsc("SVO");
            assertThat(found).hasSize(34);
            assertThat(found.subList(0, 10))
                    .extracting(Route::getRouteNo, r -> r.getValidity().substring(0, 12),
                                r -> r.getArrivalAirportData().getAirportCode(),
                                r -> r.getAirplaneData().getAirplaneCode())
                    .containsExactly(
                            tuple("PG0006", "[\"2025-10-01", "KJA", "789"),
                            tuple("PG0006", "[\"2025-11-01", "KJA", "E70"),
                            tuple("PG0006", "[\"2025-12-01", "KJA", "339"),
                            tuple("PG0006", "[\"2026-01-01", "KJA", "32N"),
                            tuple("PG0012", "[\"2025-10-01", "UFA", "351"),
                            tuple("PG0012", "[\"2025-11-01", "UFA", "E70"),
                            tuple("PG0012", "[\"2025-12-01", "UFA", "E70"),
                            tuple("PG0012", "[\"2026-01-01", "UFA", "E70"),
                            tuple("PG0018", "[\"2025-10-01", "KZN", "339"),
                            tuple("PG0018", "[\"2025-11-01", "KZN", "7M7"));
            assertThat(found).allSatisfy(r -> {
                assertThat(r.getDepartureAirportData().getAirportCode()).isEqualTo("SVO");
            });
        }

        /**
         * Asserts that an airport finds routes from that airport only, sorted by number and then by validity, whose
         * ids agree with the inherited attributes over the same columns.
         *
         * @param departureAirport the airport's code.
         */
        @ValueSource(strings = {"SVO", "DME", "LED"})
        @ParameterizedTest
        @Transactional
        void __(final String departureAirport) {
            final var found = repositoryInstance().findAllByDepartureAirportOrderByRouteNoAscValidityAsc(
                    departureAirport);
            assertThat(found)
                    .isNotEmpty()
                    .allSatisfy(r -> {
                        assertThat(r.getDepartureAirportData().getAirportCode()).isEqualTo(departureAirport);
                        assertThat(r.getId()).isEqualTo(RouteId.of(r.getRouteNo(), r.getValidity()));
                    })
                    .isSortedAccordingTo(Comparator.comparing(Route::getRouteNo).thenComparing(Route::getValidity));
        }

        /**
         * Asserts that a {@code null} airport is rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullDepartureAirport() {
            assertThatThrownBy(() -> repositoryInstance().findAllByDepartureAirportOrderByRouteNoAscValidityAsc(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /**
     * Tests {@link RouteRepository#listByRouteNo(String, Pageable)} ({@code DEMODB_ROUTES_I_01}).
     */
    @Nested
    class ListByRouteNo_Test {

        /**
         * Asserts that the exercise's route, {@code PG0001}, lists its four periods, by validity, each with its
         * airplane and its days of the week, as the exercise's expected output shows.
         */
        @Test
        @Transactional
        void __Exercise() {
            final var page = repositoryInstance().listByRouteNo("PG0001", Pageable.unpaged(BY_VALIDITY));
            assertThat(page.getContent())
                    .extracting(r -> r.getValidity().substring(0, 12), r -> r.getAirplaneData().getAirplaneCode(),
                                Route::getDaysOfWeek)
                    .containsExactly(
                            tuple("[\"2025-10-01", "339", java.util.List.of(1, 2, 3, 5, 6, 7)),
                            tuple("[\"2025-11-01", "789", java.util.List.of(1, 2, 3, 5, 6, 7)),
                            tuple("[\"2025-12-01", "789", java.util.List.of(2, 3, 4, 5, 6, 7)),
                            tuple("[\"2026-01-01", "77W", java.util.List.of(2, 4, 6, 7)));
            assertThat(page.getTotalElements()).isEqualTo(4L);
        }

        /**
         * Asserts that a route number lists periods of that number only, sorted by validity, which do not overlap:
         * each period's upper bound is the next one's lower bound, as the table's exclusion constraint keeps them.
         *
         * @param routeNo the route number.
         */
        @ValueSource(strings = {"PG0001", "PG0006", "PG0012"})
        @ParameterizedTest
        void __(final String routeNo) {
            final var page = repositoryInstance().listByRouteNo(routeNo, Pageable.unpaged(BY_VALIDITY));
            final var periods = page.getContent();
            assertThat(periods)
                    .isNotEmpty()
                    .allSatisfy(r -> assertThat(r.getRouteNo()).isEqualTo(routeNo))
                    .isSortedAccordingTo(Comparator.comparing(Route::getValidity));
            for (int i = 1; i < periods.size(); i++) {
                final var previous = periods.get(i - 1).getValidity();
                final var current = periods.get(i).getValidity();
                assertThat(current.substring(2, current.indexOf(",") - 1))
                        .isEqualTo(previous.substring(previous.indexOf(",") + 2, previous.length() - 2));
            }
        }

        /**
         * Asserts that pages of three by validity walk the four periods of {@code PG0001}, as many in all as the
         * count query says.
         */
        @Test
        void __Paged() {
            final var page0 = repositoryInstance().listByRouteNo("PG0001", PageRequest.of(0, 3, BY_VALIDITY));
            assertThat(page0.getContent()).hasSize(3);
            assertThat(page0.getTotalElements()).isEqualTo(4L);
            final var page1 = repositoryInstance().listByRouteNo("PG0001", page0.nextPageable());
            assertThat(page1.getContent())
                    .extracting(r -> r.getValidity().substring(0, 12))
                    .containsExactly("[\"2026-01-01");
            assertThat(page1.hasNext()).isFalse();
        }

        /**
         * Asserts that a {@code null} route number, and a {@code null} pageable, are rejected before any query runs.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_Null() {
            assertThatThrownBy(() -> repositoryInstance().listByRouteNo(null, Pageable.unpaged()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repositoryInstance().listByRouteNo("PG0001", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
