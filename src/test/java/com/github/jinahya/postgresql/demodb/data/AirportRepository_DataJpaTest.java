package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link AirportRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. On the H2 the {@code airports} view is a table generated from its
 * entity, whose columns, the code apart, are read-only, so a row saved through the fixtures carries its code and
 * nothing else; the exercises on the countries therefore run against an empty table, which still proves their
 * queries valid and runnable on the H2.
 *
 * @see AirportRepository_SpringBootIT
 */
class AirportRepository_DataJpaTest
        extends _Repository_DataJpaTest<AirportRepository, Airport, String> {

    /**
     * Creates a new instance.
     */
    AirportRepository_DataJpaTest() {
        super(AirportRepository.class, Airport.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link AirportRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().airport();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirportRepository#findAllByCountryOrderByAirportCodeAsc(String)} ({@code DEMODB_AIRPORTS_B_01}).
     */
    @Nested
    class FindAllByCountryOrderByAirportCodeAsc_Test {

        /**
         * Saves an airport, whose country the H2 cannot hold, and asserts that a country finds an empty list: the
         * query runs, and matches no row.
         */
        @Test
        void _Empty_Unknown() {
            fixtures().airport();
            assertThat(repositoryInstance().findAllByCountryOrderByAirportCodeAsc(UUID.randomUUID().toString()))
                    .isEmpty();
        }

        /**
         * Asserts that a {@code null} country is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
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
         * Asserts that the grouped select runs on the H2 and selects an empty page from the empty table, with no
         * countries in all.
         */
        @Test
        void _Empty_NoAirports() {
            final var page = repositoryInstance().selectAirportCountsByCountry(Pageable.unpaged());
            assertThat(page).isEmpty();
            assertThat(page.getTotalElements()).isZero();
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
