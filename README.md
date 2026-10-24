# postgresql-demodb-spring-data

Spring Data repositories for the [PostgreSQL Demonstration Database](https://postgrespro.com/community/demodb)
(Airlines; database `demo`, schema `bookings`), built on the mapped superclasses of
[postgresql-demodb-persistence](https://github.com/jinahya/postgresql-demodb-persistence).

The sibling [postgresql-demodb-jakarta-data](https://github.com/jinahya/postgresql-demodb-jakarta-data) is the same
thing in Jakarta Data.

## The entities

Upstream publishes mapped superclasses (`MappedFlight`, ...), their static metamodel and two converters, and keeps its
entities in its test sources: which associations to map, and how, is an application's decision. This project is that
application. Its entities, in `com.github.jinahya.postgresql.demodb.persistence`, one per table and view, are copies
of upstream's test entities, kept in step with them. One repository per entity, in
`com.github.jinahya.postgresql.demodb.data`, each extending `_Repository<E, ID>` (`JpaRepository` +
`JpaSpecificationExecutor`); the three views (`airplanes`, `airports`, `timetable`) are entities too, so they have
repositories like the tables.

## Requirements

* JDK 25 to build (enforced); the published jar targets 17, as upstream does
* Maven 3.x; there is no wrapper here
* Spring Boot 4.1.x, Spring Data JPA (`provided`: bring it, and a JPA provider, yourself)

The upstream jar, and its `test-companion` jar of randomizers and persisters, are `0.0.1-SNAPSHOT`s that exist only
locally. Install them first:

```sh
(cd ../postgresql-demodb-persistence && ./mvnw -DskipTests install)
```

## Build

```sh
mvn clean install                                   # unit tests only (embedded H2 in PostgreSQL mode)
mvn -Dtest=FlightRepository_DataJpaTest test
```

### Integration tests

The `*_SpringBootIT` classes run against the real demonstration database and are not run by `install`/`test`. Start
the container that upstream provides, which serves `demo` on `localhost:5432` as `postgres` / `password` with three
months of flights loaded, then name the tests:

```sh
../postgresql-demodb-persistence/_docker_compose_up.sh                           # Ctrl-C takes it down
mvn -Dtest='*_SpringBootIT' -Dsurefire.failIfNoSpecifiedTests=false test
```

`DEMODB_PASSWORD` overrides the password. The tests only read the sample data, and never generate DDL. The session's
time zone is UTC, as the container's own `psql` sessions have it, so a month or a day a query takes out of a
`timestamptz` is the one upstream's exercises show.

## The exercises

Upstream's [EXERCISES.asciidoc](https://github.com/jinahya/postgresql-demodb-persistence/blob/develop/EXERCISES.asciidoc)
states the exercises on the installed data, each with an id (`DEMODB_FLIGHTS_B_01`, ...). Each repository here
answers the Basic and the Intermediate pick of its section, one method per exercise, the id named in the method's
javadoc.

## Links

### [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)

* [Defining Query Methods](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html)
* [Specifications](https://docs.spring.io/spring-data/jpa/reference/jpa/specifications.html)

### [Spring Boot](https://docs.spring.io/spring-boot/reference/)

* [Auto-configured Data JPA Tests](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.autoconfigured-spring-data-jpa)
