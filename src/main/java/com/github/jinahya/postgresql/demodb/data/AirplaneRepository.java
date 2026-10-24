package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import com.github.jinahya.postgresql.demodb.persistence.Airplane_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link Airplane}, the {@code airplanes} view of the demonstration database.
 * <p>
 * The view shows the {@code airplanes_data} table with its names in the session's language, so every column but the
 * id, the airplane's code, is read-only. The methods below answer the exercises of upstream's
 * {@code EXERCISES.asciidoc} on the {@code airplanes} view, one method per exercise, named in each method's javadoc
 * by the exercise's id ({@code DEMODB_AIRPLANES_B_01}, ...). A Basic exercise is a derived query, with the order the
 * exercise asks for in its name, and returns a {@link List}, with no {@link Pageable}. An Intermediate one takes a
 * {@link Pageable}, the page, and returns a {@link Page}; the order the exercise asks for is in the query, and its
 * {@code LIMIT} is the size of the caller's page. A result that is not an entity comes back as a
 * {@link jakarta.persistence.Tuple}, each element read by its alias, with its type.
 *
 * @see Airplane
 * @see Airplane_
 */
@Repository
public interface AirplaneRepository
        extends _Repository<Airplane, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds every airplane of the view, by {@link Airplane_#AIRPLANE_CODE} ({@code DEMODB_AIRPLANES_B_01}, which
     * orders by {@link Airplane_#AIRPLANE_CODE}).
     * <p>
     * The query, and its order, are derived from the method name. The view holds ten rows, which the exercise lists
     * whole, so there is no {@link Pageable}.
     *
     * @return a list of every airplane, by code; empty if there is none.
     */
    List<Airplane> findAllByOrderByAirplaneCodeAsc();

    /**
     * Selects the makers, the first word of each airplane's {@link Airplane_#MODEL}, with how many airplanes each
     * has ({@code DEMODB_AIRPLANES_I_01}, which orders by {@code airplanes} descending, then by {@code maker}; the
     * exercise shows every row, so the caller's page holds them all).
     * <p>
     * The maker is the model up to its first space, which JPQL takes with {@code SUBSTRING} and {@code LOCATE}; the
     * airplanes are grouped by it and counted. The count query counts the distinct makers.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the makers with their numbers of airplanes, each a tuple of {@code maker} ({@link String})
     *         and {@code airplanes} ({@link Long}).
     */
    @Query(value = """
            SELECT SUBSTRING(a.model, 1, LOCATE(' ', a.model) - 1) AS maker, COUNT(a) AS airplanes
            FROM Airplane a
            GROUP BY SUBSTRING(a.model, 1, LOCATE(' ', a.model) - 1)
            ORDER BY COUNT(a) DESC, SUBSTRING(a.model, 1, LOCATE(' ', a.model) - 1)""",
           countQuery = """
                   SELECT COUNT(DISTINCT SUBSTRING(a.model, 1, LOCATE(' ', a.model) - 1))
                   FROM Airplane a""")
    Page<Tuple> selectAirplaneCountsByMaker(Pageable pageable);
}
