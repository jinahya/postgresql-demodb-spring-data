package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.AirplaneData;
import com.github.jinahya.postgresql.demodb.persistence.AirplaneData_;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository for {@link AirplaneData}, the {@code airplanes_data} table of the demonstration database.
 * <p>
 * The id is the airplane's code, a {@link String} assigned by whoever creates the row, so a new airplane is saved
 * with its code set. The methods below answer the exercises of upstream's {@code EXERCISES.asciidoc} on the
 * {@code airplanes_data} table, one method per exercise, named in each method's javadoc by the exercise's id
 * ({@code DEMODB_AIRPLANES_DATA_B_01}, ...). A Basic exercise is a derived query, with the order the exercise asks
 * for in its name, and returns a {@link List}, with no {@link Pageable}. An Intermediate one takes a
 * {@link Pageable}, the page, and returns a {@link Page}; the order the exercise asks for is in the query, and its
 * {@code LIMIT} is the size of the caller's page. Such a query selects the entity whole ({@code SELECT a AS
 * airplane}); what is computed is a column of its own beside it, and the result comes back as a
 * {@link jakarta.persistence.Tuple}, each element read by its alias, with its type
 * ({@code tuple.get("airplane", AirplaneData.class)}).
 *
 * @see AirplaneData
 * @see AirplaneData_
 */
@Repository
public interface AirplaneDataRepository
        extends _Repository<AirplaneData, String> {

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Finds every airplane, by {@link AirplaneData_#AIRPLANE_CODE} ({@code DEMODB_AIRPLANES_DATA_B_01}, which orders
     * by {@link AirplaneData_#AIRPLANE_CODE}).
     * <p>
     * The query, and its order, are derived from the method name. The table holds ten rows, which the exercise lists
     * whole, so there is no {@link Pageable}.
     *
     * @return a list of every airplane, by code; empty if there is none.
     */
    List<AirplaneData> findAllByOrderByAirplaneCodeAsc();

    /**
     * Selects every airplane with the hours it flies at its full range, the longest first
     * ({@code DEMODB_AIRPLANES_DATA_I_02}, which orders by {@code hours} descending, then by
     * {@link AirplaneData_#AIRPLANE_CODE}; the exercise shows every row, so the caller's page holds them all).
     * <p>
     * The hours are the range over the speed, as a decimal; the exercise rounds them to two places, which a caller
     * does when it shows them. The order is in the query; a sort of the pageable comes after it.
     *
     * @param pageable the page to return; must not be {@code null}.
     * @return a page of the airplanes with their hours, each a tuple of {@code airplane} ({@link AirplaneData}) and
     *         {@code hours} (a {@link Number}, as the provider gives the quotient).
     */
    @Query(value = """
            SELECT a AS airplane, a.range * 1.0 / a.speed AS hours
            FROM AirplaneData a
            ORDER BY a.range * 1.0 / a.speed DESC, a.airplaneCode""",
           countQuery = """
                   SELECT COUNT(a)
                   FROM AirplaneData a""")
    Page<Tuple> selectHoursAtFullRange(Pageable pageable);
}
