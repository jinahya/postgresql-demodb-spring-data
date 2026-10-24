package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import com.github.jinahya.postgresql.demodb.persistence.Route_;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link RouteRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. Each test persists the airports, airplanes and routes it needs,
 * through the fixtures and the repository under test, with the route numbers and the validity periods it is about,
 * and the slice rolls them back when it ends. A route's id is what writes its number and its period, so a test
 * builds the id itself; the inherited read-only attributes over the same columns, and over the airports' codes,
 * are not loaded into a saved instance, so the tests assert through the ids and the associations.
 *
 * @see RouteRepository_SpringBootIT
 */
class RouteRepository_DataJpaTest
        extends _Repository_DataJpaTest<RouteRepository, Route, RouteId> {

    /**
     * A sort by validity; by the attribute's name, since the metamodel's {@code Route_.validity} is filled in only
     * once the context is up, after this class is loaded.
     */
    private static final Sort BY_VALIDITY = Sort.by(Sort.Direction.ASC, Route_.VALIDITY);

    /**
     * Creates a new instance.
     */
    RouteRepository_DataJpaTest() {
        super(RouteRepository.class, Route.class, RouteId.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns a route number no installed route has: {@code PG} and four lower-case letters.
     *
     * @return a random route number.
     */
    private static String randomRouteNo() {
        return "PG" + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }

    /**
     * Returns the validity text of the specified month, as the randomizers write it.
     *
     * @param month the month.
     * @return a {@code tstzrange} literal of {@code month}.
     */
    private static String validityOf(final YearMonth month) {
        return "[" + month.atDay(1) + " 00:00:00+00," + month.plusMonths(1).atDay(1) + " 00:00:00+00)";
    }

    /**
     * Saves a randomized route of the specified number, valid in the specified month, from the specified airport.
     *
     * @param routeNo   the route number.
     * @param month     the month of the validity.
     * @param departure the airport of departure.
     * @return the saved route, managed in the test's persistence context.
     */
    private Route saveRoute(final String routeNo, final YearMonth month, final AirportData departure) {
        final var route = _Repository_DataJpaTest_Fixtures.randomized(Route.class);
        route.setRouteNo(routeNo);
        route.setValidity(validityOf(month));
        route.setId(RouteId.of(routeNo, validityOf(month)));
        route.setDepartureAirportData(departure);
        route.setArrivalAirportData(fixtures().airportData());
        route.setAirplaneData(fixtures().airplaneData());
        return repositoryInstance().save(route);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link RouteRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().route();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link RouteRepository#findAllByDepartureAirportOrderByRouteNoAscValidityAsc(String)}
     * ({@code DEMODB_ROUTES_B_01}).
     */
    @Nested
    class FindAllByDepartureAirportOrderByRouteNoAscValidityAsc_Test {

        /**
         * Asserts that an airport no route departs from finds an empty list.
         */
        @Test
        void _Empty_Unknown() {
            final var found = repositoryInstance().findAllByDepartureAirportOrderByRouteNoAscValidityAsc(
                    fixtures().airportData().getAirportCode());
            assertThat(found).isEmpty();
        }

        /**
         * Saves three routes from an airport, two of one number in two months and one of another number, and one
         * route from another airport, and asserts that the airport finds the three, by number and then by validity,
         * as the exercise sorts.
         */
        @Test
        void __() {
            final var departure = fixtures().airportData();
            final var first = randomRouteNo();
            final var second = randomRouteNo();
            final var lower = first.compareTo(second) < 0 ? first : second;
            final var higher = lower.equals(first) ? second : first;
            final var november = YearMonth.of(2031, 11);
            final var december = YearMonth.of(2031, 12);
            final var higherNovember = saveRoute(higher, november, departure);
            final var lowerDecember = saveRoute(lower, december, departure);
            final var lowerNovember = saveRoute(lower, november, departure);
            final var other = saveRoute(randomRouteNo(), november, fixtures().airportData());
            final var found = repositoryInstance().findAllByDepartureAirportOrderByRouteNoAscValidityAsc(
                    departure.getAirportCode());
            assertThat(found)
                    .containsExactly(lowerNovember, lowerDecember, higherNovember)
                    .doesNotContain(other)
                    .allSatisfy(r -> assertThat(r.getDepartureAirportData()).isSameAs(departure));
        }

        /**
         * Asserts that a {@code null} airport is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Asserts that a route number no route has lists an empty page.
         */
        @Test
        void _Empty_Unknown() {
            final var page = repositoryInstance().listByRouteNo(randomRouteNo(), Pageable.unpaged(BY_VALIDITY));
            assertThat(page).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        /**
         * Saves a route number's periods in three months, out of order, and a period of another number, and asserts
         * that the number lists its three periods, by validity as the exercise sorts.
         */
        @Test
        void __() {
            final var routeNo = randomRouteNo();
            final var departure = fixtures().airportData();
            final var start = YearMonth.of(2031, ThreadLocalRandom.current().nextInt(1, 10));
            final var third = saveRoute(routeNo, start.plusMonths(2), departure);
            final var first = saveRoute(routeNo, start, departure);
            final var second = saveRoute(routeNo, start.plusMonths(1), departure);
            final var other = saveRoute(randomRouteNo(), start, departure);
            final var page = repositoryInstance().listByRouteNo(routeNo, Pageable.unpaged(BY_VALIDITY));
            assertThat(page.getContent())
                    .containsExactly(first, second, third)
                    .doesNotContain(other)
                    .allSatisfy(r -> assertThat(r.getId().getRouteNo()).isEqualTo(routeNo));
        }

        /**
         * Saves three periods of a route number, and asserts that pages of two by validity split them two and one,
         * three in all, which the count query says.
         */
        @Test
        void __Paged() {
            final var routeNo = randomRouteNo();
            final var departure = fixtures().airportData();
            for (int i = 0; i < 3; i++) {
                saveRoute(routeNo, YearMonth.of(2032, 1 + i), departure);
            }
            final var page0 = repositoryInstance().listByRouteNo(routeNo, PageRequest.of(0, 2, BY_VALIDITY));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
            final var page1 = repositoryInstance().listByRouteNo(routeNo, page0.nextPageable());
            assertThat(page1.getContent()).hasSize(1);
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
            assertThatThrownBy(() -> repositoryInstance().listByRouteNo(randomRouteNo(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
