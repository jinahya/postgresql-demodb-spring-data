package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirportData;
import com.github.jinahya.postgresql.demodb.persistence.AirportData_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link AirportData}, the {@code airports_data} table of the demonstration database.
 * <p>
 * The id is the airport's code, a {@link String} assigned by whoever creates the row. The methods below answer the
 * exercises of upstream's {@code EXERCISES.asciidoc} on the {@code airports_data} table, one method per exercise,
 * named in each method's javadoc by the exercise's id ({@code DEMODB_AIRPORTS_DATA_B_01}, ...). A Basic exercise is
 * a derived query, with the order the exercise asks for in its name, and returns a {@link List}, with no
 * {@link Pageable}. An Intermediate one takes a {@link Pageable}, the page, and returns a {@link Page}; the order the
 * exercise asks for is in the query, and its {@code LIMIT} is the size of the caller's page. A result that is not an
 * entity comes back as a {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see AirportData
 * @see AirportData_
 */
@Repository
public interface AirportDataRepository
        extends _Repository<AirportData, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds the airports in the specified time zone, by {@link AirportData_#AIRPORT_CODE}
     * ({@code DEMODB_AIRPORTS_DATA_B_01}, which asks for the airports in {@code Europe/Moscow}, ordered by
     * {@link AirportData_#AIRPORT_CODE}).
     * <p>
     * The query, and its order, are derived from the method name. The exercise lists the matches whole, so there is
     * no {@link Pageable}.
     *
     * @param timezone the value of the {@link AirportData_#TIMEZONE} attribute to match; must not be {@code null}.
     * @return a list of the airports in {@code timezone}, by code; empty if there is none.
     */
    List<AirportData> findAllByTimezoneOrderByAirportCodeAsc(String timezone);

    /**
     * Selects the time zones with how many airports each has, the most first ({@code DEMODB_AIRPORTS_DATA_I_02},
     * which orders by {@code airports} descending, then by {@code timezone}; the exercise's {@code LIMIT 10} is the
     * first page of ten).
     * <p>
     * The airports are grouped by time zone and counted; the count query counts the distinct time zones.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the time zones with their numbers of airports, each a tuple of {@code timezone}
     *         ({@link String}) and {@code airports} ({@link Long}).
     */
    @Query(value = """
            SELECT a.timezone AS timezone, COUNT(a) AS airports
            FROM AirportData a
            GROUP BY a.timezone
            ORDER BY COUNT(a) DESC, a.timezone""",
           countQuery = """
                   SELECT COUNT(DISTINCT a.timezone)
                   FROM AirportData a""")
    Page<Tuple> selectAirportCountsByTimezone(Pageable pageable);
}
