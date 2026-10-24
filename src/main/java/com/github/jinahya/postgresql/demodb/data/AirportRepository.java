package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
import com.github.jinahya.postgresql.demodb.persistence.Airport_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link Airport}, the {@code airports} view of the demonstration database.
 * <p>
 * The view shows the {@code airports_data} table with its names in the session's language, so every column but the
 * id, the airport's code, is read-only. The methods below answer the exercises of upstream's
 * {@code EXERCISES.asciidoc} on the {@code airports} view, one method per exercise, named in each method's javadoc
 * by the exercise's id ({@code DEMODB_AIRPORTS_B_01}, ...). A Basic exercise is a derived query, with the order the
 * exercise asks for in its name, and returns a {@link List}, with no {@link Pageable}. An Intermediate one takes a
 * {@link Pageable}, the page, and returns a {@link Page}; the order the exercise asks for is in the query, and its
 * {@code LIMIT} is the size of the caller's page. A result that is not an entity comes back as a
 * {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see Airport
 * @see Airport_
 */
@Repository
public interface AirportRepository
        extends _Repository<Airport, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the airports of the specified country, by {@link Airport_#AIRPORT_CODE} ({@code DEMODB_AIRPORTS_B_01},
     * which asks for the airports of {@code Norway}, ordered by {@link Airport_#AIRPORT_CODE}).
     * <p>
     * The query, and its order, are derived from the method name. The country is in the session's language, as the
     * view gives it. The exercise lists the matches whole, so there is no {@link Pageable}.
     *
     * @param country the value of the {@link Airport_#COUNTRY} attribute to match; must not be {@code null}.
     * @return a list of the airports of {@code country}, by code; empty if there is none.
     */
    List<Airport> findAllByCountryOrderByAirportCodeAsc(String country);

    /**
     * Selects the countries with how many airports each has, the most first ({@code DEMODB_AIRPORTS_I_01}, which
     * orders by {@code airports} descending, then by {@code country}; the exercise's {@code LIMIT 10} is the first
     * page of ten).
     * <p>
     * The airports are grouped by country and counted; the count query counts the distinct countries.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the countries with their numbers of airports, each a tuple of {@code country}
     *         ({@link String}) and {@code airports} ({@link Long}).
     */
    @Query(value = """
            SELECT a.country AS country, COUNT(a) AS airports
            FROM Airport a
            GROUP BY a.country
            ORDER BY COUNT(a) DESC, a.country""",
           countQuery = """
                   SELECT COUNT(DISTINCT a.country)
                   FROM Airport a""")
    Page<Tuple> selectAirportCountsByCountry(Pageable pageable);
}
