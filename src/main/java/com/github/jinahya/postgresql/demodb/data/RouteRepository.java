package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Route;
import com.github.jinahya.postgresql.demodb.persistence.RouteId;
import com.github.jinahya.postgresql.demodb.persistence.Route_;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

/**
 * A repository for {@link Route}, the {@code routes} table of the demonstration database.
 * <p>
 * The table has no primary key; the entity names one of its own, {@link RouteId}, the route number and the validity
 * period, the pair the table's exclusion constraint keeps apart. The same two columns are also the read-only
 * attributes {@link Route_#ROUTE_NO} and {@link Route_#VALIDITY}, which the queries below name. The methods below
 * answer the exercises of upstream's {@code EXERCISES.asciidoc} on the {@code routes} table, one method per
 * exercise, named in each method's javadoc by the exercise's id ({@code DEMODB_ROUTES_B_01}, ...). A Basic exercise
 * is a derived query, with the order the exercise asks for in its name, and returns a {@link List}, with no
 * {@link Pageable}. An Intermediate one takes a {@link Pageable}, the page and the sort, and returns a {@link Page};
 * an exercise that shows rows is a {@code default} method, {@code list...}, which builds a {@link Specification}
 * with the Criteria API and runs it through {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor}.
 *
 * @see Route
 * @see Route_
 */
@Repository
public interface RouteRepository
        extends _Repository<Route, RouteId> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the routes departing from the specified airport, by {@link Route_#ROUTE_NO} and then by
     * {@link Route_#VALIDITY} ({@code DEMODB_ROUTES_B_01}, which asks for the routes from Sheremetyevo, {@code SVO},
     * ordered by {@link Route_#ROUTE_NO}, then by {@link Route_#VALIDITY}).
     * <p>
     * The query, and its order, are derived from the method name, over the read-only {@link Route_#DEPARTURE_AIRPORT}
     * attribute, the code of the airport. The validity is a {@code tstzrange}, which the entity carries as its text;
     * the database orders the column itself, by its lower bound. The exercise lists the matches whole, so there is no
     * {@link Pageable}.
     *
     * @param departureAirport the value of the {@link Route_#DEPARTURE_AIRPORT} attribute to match; must not be
     *                         {@code null}.
     * @return a list of the routes from {@code departureAirport}, by route number and then by validity; empty if
     *         there is none.
     */
    List<Route> findAllByDepartureAirportOrderByRouteNoAscValidityAsc(String departureAirport);

    /**
     * Lists the periods of the specified route number, each a row with its own validity, airplane and days of the
     * week ({@code DEMODB_ROUTES_I_01}, which asks for the periods of {@code PG0001}, ordered by
     * {@link Route_#VALIDITY}; the exercise shows every row, so the caller's page holds them all).
     * <p>
     * A {@link Specification} of one predicate, on {@link Route_#ROUTE_NO}. The exercise shows the bounds of each
     * period, which are the validity's text here, {@code ["2025-10-01 00:00:00+00","2025-11-01 00:00:00+00")}. The
     * query has no order of its own, so give {@code pageable} the exercise's sort, by {@link Route_#VALIDITY}.
     *
     * @param routeNo  the value of the {@link Route_#ROUTE_NO} attribute to match; must not be {@code null}.
     * @param pageable the page to return, and its sort; must not be {@code null}.
     * @return a page of the periods of {@code routeNo}.
     */
    default Page<Route> listByRouteNo(final String routeNo, final Pageable pageable) {
        Objects.requireNonNull(routeNo, "routeNo is null");
        Objects.requireNonNull(pageable, "pageable is null");
        final Specification<Route> specification = (r, q, b) -> b.equal(r.get(Route_.routeNo), routeNo);
        return findAll(specification, pageable);
    }
}
