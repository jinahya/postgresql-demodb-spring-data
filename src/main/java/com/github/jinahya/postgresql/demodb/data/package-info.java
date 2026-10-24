/**
 * Spring Data repositories for the entities of the PostgreSQL Demonstration Database, which live in the sibling
 * package {@code com.github.jinahya.postgresql.demodb.persistence}: this project's own entities, each extending a mapped
 * superclass of the upstream {@code postgres-demonstration-persistence} artifact.
 * <p>
 * The package is {@link NullMarked}: every parameter and return type here is non-null unless marked {@code @Nullable}.
 * Spring Data reads that from the repository interfaces, and rejects a {@code null} argument to a query method with an
 * {@link IllegalArgumentException} before any query runs. A lookup that may find nothing returns an
 * {@link java.util.Optional} instead of a nullable entity.
 * <p>
 * Every repository here is a subinterface of {@link com.github.jinahya.postgresql.demodb.data._Repository}, one per entity, the
 * views of the database included, since each view is an entity of its own; Spring Data JPA implements them.
 */
@NullMarked
package com.github.jinahya.postgresql.demodb.data;

import org.jspecify.annotations.NullMarked;
