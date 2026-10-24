package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Airplane;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link AirplaneRepository} in a JPA slice, over an embedded H2, through {@link _Repository_DataJpaTest}.
 * <p>
 * See that class for how the slice is configured. On the H2 the {@code airplanes} view is a table generated from
 * its entity, whose columns, the code apart, are read-only, so a row saved through the fixtures carries its code and
 * nothing else; the exercise on the models therefore runs against an empty table, which still proves its query
 * valid and runnable on the H2.
 *
 * @see AirplaneRepository_SpringBootIT
 */
class AirplaneRepository_DataJpaTest
        extends _Repository_DataJpaTest<AirplaneRepository, Airplane, String> {

    /**
     * Creates a new instance.
     */
    AirplaneRepository_DataJpaTest() {
        super(AirplaneRepository.class, Airplane.class, String.class);
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Tests {@link AirplaneRepository#findById(Object)}.
     */
    @Nested
    class FindById_Test {

        /**
         * Saves a row through the fixtures, and asserts that finding it by its id returns that very instance, from
         * the test's persistence context.
         */
        @Test
        void _NotEmpty_Known() {
            final var saved = fixtures().airplane();
            final var found = repositoryInstance().findById(identifier(saved));
            assertThat(found).hasValueSatisfying(v -> assertThat(v).isSameAs(saved));
        }
    }

    // ----------------------------------------------------------------------------------------------------- exercises

    /**
     * Tests {@link AirplaneRepository#findAllByOrderByAirplaneCodeAsc()} ({@code DEMODB_AIRPLANES_B_01}).
     */
    @Nested
    class FindAllByOrderByAirplaneCodeAsc_Test {

        /**
         * Asserts that an empty table finds an empty list.
         */
        @Test
        void _Empty_NoAirplanes() {
            assertThat(repositoryInstance().findAllByOrderByAirplaneCodeAsc()).isEmpty();
        }

        /**
         * Saves three airplanes, and asserts that they are found, those very instances, by code, as the exercise
         * sorts.
         */
        @Test
        void __() {
            final var saved = Stream.of(fixtures().airplane(), fixtures().airplane(), fixtures().airplane())
                    .sorted(Comparator.comparing(Airplane::getAirplaneCode))
                    .toList();
            final var found = repositoryInstance().findAllByOrderByAirplaneCodeAsc();
            assertThat(found)
                    .containsExactlyElementsOf(saved)
                    .anySatisfy(a -> assertThat(a).isSameAs(saved.getFirst()));
        }
    }

    /**
     * Tests {@link AirplaneRepository#selectAirplaneCountsByMaker(Pageable)} ({@code DEMODB_AIRPLANES_I_01}).
     */
    @Nested
    class SelectAirplaneCountsByMaker_Test {

        /**
         * Asserts that the grouped select, with its {@code SUBSTRING} and {@code LOCATE}, runs on the H2 and selects
         * an empty page from the empty table, with no makers in all.
         */
        @Test
        void _Empty_NoAirplanes() {
            final var page = repositoryInstance().selectAirplaneCountsByMaker(Pageable.unpaged());
            assertThat(page).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }

        /**
         * Asserts that a {@code null} pageable is rejected before any query runs: the package is
         * {@code @NullMarked}, so Spring Data checks the arguments, and throws {@link IllegalArgumentException}.
         */
        @Test
        @SuppressWarnings({"DataFlowIssue", "NullAway"})
        void _IllegalArgumentException_NullPageable() {
            assertThatThrownBy(() -> repositoryInstance().selectAirplaneCountsByMaker(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
