# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

Spring Data repositories over the Jakarta Persistence mappings of
[`postgresql-demodb-persistence`](https://github.com/jinahya/postgresql-demodb-persistence), which maps the
[PostgreSQL Demonstration Database](https://postgrespro.com/community/demodb) (Airlines; database `demo`, schema
`bookings`). A single, parent-less jar, modelled on the sibling `mysql-sakila-spring-data`.

- Main package: `com.github.jinahya.postgresql.demodb.data` (`@NullMarked`, JSpecify: Spring Data rejects a `null`
  argument to a repository method with `IllegalArgumentException` before any query runs; a lookup that may find
  nothing returns `Optional`).
- Entities: `com.github.jinahya.postgresql.demodb.persistence`, **in this project's `src/main`**, one per table and
  view (`Airplane`, `AirplaneData`, `Airport`, `AirportData`, `BoardingPass`, `Booking`, `Flight`, `Route`, `Seat`,
  `Segment`, `Ticket`, `Timetable`, with the id classes `BoardingPassId`, `RouteId`, `SeatId`, `SegmentId`). Each
  extends a mapped superclass of the upstream artifact `io.github.jinahya:postgres-demonstration-persistence`
  (`MappedFlight`, ...). The three views are entities too, so they have repositories like the tables.
- One repository per entity, named `<Entity>Repository`, extending `_Repository<E, ID>` (`JpaRepository` +
  `JpaSpecificationExecutor`, `@NoRepositoryBean`).
- The published jar carries **the entities and the repositories**: `spring-data-jpa`, `jakarta.persistence-api` and
  `jakarta.validation-api` are `provided`, so a consumer brings Spring Data JPA, a JPA provider, and (to enforce the
  constraints on repository parameters) a validation provider plus `@Validated`. No Boot starter is named in main.

## Upstream, and the entities

The sibling checkout `../postgresql-demodb-persistence` publishes, at `0.0.1-SNAPSHOT`, a jar of **mapped
superclasses, their generated static metamodel (`MappedFlight_`, ...) and two converters**
(`IntegerListArrayConverter` for `integer[]`, `IntervalConverter` for `interval`), and a second jar of the same
artifact classified `test-companion`, holding the `*_Randomizer` and `*_Persister` classes compiled with its tests,
and the `__Random_TestUtils` helper they draw their values from. Both are **SNAPSHOTs that exist only locally**:
after a change upstream, run `./mvnw -DskipTests install` there before building here.

- **Upstream keeps its entities in `src/test`**, on purpose: which associations an entity maps, lazy or not, with
  what cascade, is an application's decision, and this project is that application. The entities here are copies of
  upstream's test entities, verbatim (`src/test/java/com/github/jinahya/postgresql/demodb/persistence/<Entity>.java`
  there), in the same package, so that the `test-companion` randomizers and persisters, compiled against that
  package, find them. **Keep them in step with upstream**: diff against upstream's before touching a mapping, and
  change the mapping there first.
- **Do not copy upstream's randomizers or persisters into `src/test`.** The `test-companion` jar is on the test
  classpath, and `ObjectRandomizerUtils` / `EntityPersisterUtils` find `<Entity>_Randomizer` / `<Entity>_Persister`
  next to the entity by name, package-private or not. A copy only shadows the jar and drifts.
- **Name attributes through the static metamodel** (`root.get(Route_.routeNo)`, generated here by
  `hibernate-processor` for the entities, extending upstream's `MappedRoute_`), never a string. Where a compile-time
  constant is needed, use the mapped superclass's own `ATTRIBUTE_NAME_*` constant, or the metamodel's `ROUTE_NO`.
- **The exotic columns.** `routes.days_of_week` (`integer[]`) is a `List<Integer>` and `routes.duration`
  (`interval`) a `Duration`, through upstream's converters; `tstzrange`, `point` and `jsonb` columns are `String`s
  holding the server's text. A JPQL query filters or orders by them as text; what needs the server's operators
  (`@>`, `->>`, `lower(validity)`) is native SQL, and such an exercise is native-only. Writing any of the five needs
  `stringtype=unspecified` on the JDBC URL, which `application.yaml` has.
- **Query methods, in this order of preference.** (1) A derived query, from the method name, for a plain lookup
  (`findAllByTimezoneOrderByAirportCodeAsc`). (2) A `Specification` built with the Criteria API, run through
  `JpaSpecificationExecutor` (`findAll(spec, pageable)`, `count(spec)`), as a `default` method named `list...` /
  `count...`; this covers filters, `isEmpty`, correlated subqueries. (3) **`@Query` only when a `Specification`
  cannot express it**: a projection grouped and aggregated (`GROUP BY`, `COUNT`/`SUM`/`AVG`/`EXTRACT` in the
  select); name such a method `select...`. (4) A native query (`@NativeQuery`) only for SQL that is PostgreSQL's own
  (`jsonb`'s `->>`, `tstzrange`'s `@>`, `FILTER`, window functions); such a method runs on the demonstration
  database only, so it has no slice test.
- **The order is in the query, the size is the caller's.** A Basic exercise is a derived query, its order in the
  method name, returning a `List`. An Intermediate or Advanced one takes a `Pageable` and returns a `Page<T>`; the
  exercise's `ORDER BY` is written in the query itself (`ORDER BY COUNT(a) DESC, a.timezone`), and its `LIMIT n` is
  the size of the page the caller passes. A grouped `@Query` declares its own `countQuery` (Spring Data cannot derive
  one from a grouped select); count the groups with `COUNT(DISTINCT ..)` or a subquery.
- **Results that are not entities are `jakarta.persistence.Tuple`** (in a `List`, `Stream`, `Page` or alone). The
  select takes an entity whole and aliases every item (`SELECT a AS airplane, COUNT(r) AS routes`); the caller
  reads each by its alias with its type (`tuple.get("airplane", AirplaneData.class)`,
  `tuple.get("routes", Long.class)`; a number of uncertain type as `Number`, so the same read holds on PostgreSQL
  and H2). The method's `@return` names the aliases and their types.
- **Time zones.** `timestamptz` columns are `OffsetDateTime`s, normalized to UTC on the way in. The JDBC session's
  time zone is UTC, as the container's own `psql` sessions have it, through Hikari's `connection-init-sql`
  (`SET TIME ZONE 'UTC'` in `application.yaml`), so `EXTRACT(MONTH FROM f.scheduledDeparture)` gives the month
  upstream's exercises show; a derived query compares the instants themselves and does not care. The URL's
  `options=-c TimeZone=UTC` cannot do this: the driver sends the JVM's zone as a startup parameter, which the server
  applies after the options.
- **Exercises.** Upstream's `../postgresql-demodb-persistence/EXERCISES.asciidoc` (`EXERCISES.yaml` is its
  registry) is the context, not a contract: for each repository, the Basic and the Intermediate pick of the
  exercise's section become one method each, the exercise id (`DEMODB_FLIGHTS_I_01`) named in the method's javadoc,
  and one `@Nested` test class per method in both test classes. The `SCHEMA` section is not answered.
- **Upstream getters.** The entities map single-valued associations only (`Route.getAirplaneData()`,
  `Ticket.getBooking()`, `Segment.getTicket()`/`getFlight()`, `Seat.getAirplaneData()`, `BoardingPass.getSegment()`),
  lazy, no cascade; there is no collection side. A test that needs the many side reads it through that side's
  repository with a `Specification` (`b.equal(r.get(Seat_.airplaneData), airplane)`), or a query joins the other
  entity with `JOIN Seat s ON s.airplaneData = a`.
- **No metamodel attribute in a static initializer.** `Flight_.flightId` is `null` until Hibernate builds the
  metamodel, after the test class is loaded; a `static final Sort` uses the name constant,
  `Sort.by(ASC, Flight_.FLIGHT_ID)`. `JpaSort.of(ASC, Flight_.flightId)` is fine inside a test method.
- Identifiers are assigned (`airplaneCode`, `airportCode`, `bookRef`, `ticketNo`, the composite ones), except
  `flights.flight_id`, which is `IDENTITY`. `Route` has no key of its own in the schema; its `@EmbeddedId` is
  `RouteId` (`routeNo`, `validity`), and the inherited `routeNo`/`validity` attributes are read-only there.
- Read `../postgresql-demodb-persistence/README.md` before touching anything about the mappings.

## Build

There is no wrapper here; use the Maven on `PATH`. The build JDK must be **25** (enforced; `.java-version` says so):
main sources compile at release 17, as upstream, tests at 25.

```sh
mvn test                                        # unit tests only (@DataJpaTest slices on H2)
mvn -Dtest=FlightRepository_DataJpaTest test
mvn -Dtest=SomeTest#method test
mvn -Dtest='*_SpringBootIT' -Dsurefire.failIfNoSpecifiedTests=false test   # integration tests; needs PostgreSQL, see below
mvn -o dependency:tree
mvn -o enforcer:enforce -Denforcer.rules=dependencyConvergence
```

Surefire's default includes do not match `*IT`, and no failsafe plugin is configured, so `test`/`install` never run
the `*_SpringBootIT` classes; run them explicitly with `-Dtest`.

**The integration tests need the demonstration database container** that upstream owns: run
`../postgresql-demodb-persistence/_docker_compose_up.sh` (Ctrl-C takes it down again, with its data). It runs
`onacit/postgresql-demodb:18-alpine-3m` as the container `postgresql-demodb` on `localhost:5432`, database `demo`,
user `postgres`, password `password` (`DEMODB_PASSWORD` overrides it), with three months of flights loaded on first
start, which takes about a minute. `src/test/resources/application.yaml` connects there with `ddl-auto: none` and
`PostgreSQLDialect` named outright. Never let a test generate DDL against it. A `@SpringBootTest` that runs no query
still passes with no database up, since the dialect is named; only a query proves the connection.

## Load-bearing pom decisions

Each is commented in `pom.xml`:

- **Spring Boot 4.x (`4.1.2`), not 3.x.** Boot 4 manages `jakarta.persistence-api` 3.2, which the upstream mappings are
  written against. Do not downgrade to dodge a problem.
- **`spring-boot-dependencies` is imported, not inherited** (the pom is parent-less). Every Spring / Boot / Hibernate
  version comes from the BOM; declare those with no `<version>`. Direct `dependencyManagement` entries beat the BOM,
  which is how the pins work: Lombok `1.18.48` (the BOM's breaks on newer JDKs), and `postgresql` and `h2` at
  upstream's versions, so the tests hit the same driver and the same H2 upstream generates its schema into.
- **`hibernate-processor` is on the processor path** of both compile executions, from the BOM's version, to generate
  the entities' static metamodel (`Flight_`, ...); the Hibernate it generates against is the BOM's, one patch level
  above upstream's, which the metamodel does not mind.
- **Boot 4 starters come in pairs.** Tests declare `spring-boot-starter-data-jpa-test` and `-validation-test`, not
  `spring-boot-starter-test`, which each of them brings. `@DataJpaTest` is
  `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`. Validation of repository parameters is enforced
  in tests only, through the validation starter.
- **Dependency convergence is a build defect**, not a warning. Resolve `jakarta.*` at the platform level instead of
  pinning individual API artifacts.
- **Lombok is `provided`** for main and test, on the processor path of both compile executions.

## Tests

The test root `com.github.jinahya.postgresql.demodb.data` has `___Spring_TestContext`: `@SpringBootConfiguration` +
`@EnableAutoConfiguration` + `@EntityScan(basePackageClasses = Flight.class)` (the entities are in the sibling
package). It lives in test sources on purpose: nothing in `src/main` carries a `@SpringBootApplication`. Both
`@SpringBootTest` and `@DataJpaTest` find it by searching up the packages. Anything added there reaches every test.
`SpringTestContext_SpringBootTest` and `SpringTestContext_DataJpaTest` test the context itself.

Repository tests extend a small hierarchy:

- `__Repository_TestBase<T, U, V>`: repository, entity and id types; injects the repository (`repositoryInstance()`)
  and the `EntityManager` (`entityManager()`); `identifier(entity)` reads an id through the metamodel; `column(..)`,
  `longColumn(..)`, `intColumn(..)` read a tuple's columns by alias.
- `_Repository_DataJpaTest`: `@DataJpaTest` over an embedded H2 in PostgreSQL mode (profile `h2`,
  `application-h2.yaml`, `replace: none`, so Boot's own H2, in H2's own mode, is not swapped in), rolled back per
  test. It imports `_Repository_DataJpaTest_Configuration`, which undoes the PostgreSQL settings of
  `application.yaml` for H2 (`create-drop`, `H2Dialect`, `show_sql` off), and `_Repository_DataJpaTest_Fixtures`,
  which saves randomized entities through their repositories (`fixtures().flight()`, `fixtures().seat(airplane)`,
  ...). On the H2 the three views are tables, generated from their entities, so their rows can be saved too.
- `_Repository_SpringBootIT`: `@SpringBootTest` against the installed demonstration database. Repository calls commit
  unless the test is `@Transactional`. It declares `FindById_Test` for every subclass, and `selectRandom()` /
  `selectRandomPresent()`, which aborts the test by an assumption when the table is empty.

**The injected `EntityManager` is kept, but not used.** `__Repository_TestBase` autowires it and exposes
`entityManager()`, and that stays; the base's own helpers (`identifier()`, `selectRandom()`) are its only
consumers. A repository test never calls `entityManager()` itself: no `persist`, `find`, `flush`, `createQuery`.
Everything a test does with the database goes through the repository under test, `repositoryInstance()`
(`save`, `findById`, `findAll`, ...), or the fixtures. A test that needs an entity persisted saves it through the
repository.

Naming: `<Repository>_DataJpaTest` and `<Repository>_SpringBootIT`, with **one `@Nested` class per method under
test** (`FindById_Test`, `FindAllByTimezoneOrderByAirportCodeAsc_Test`, ...), and test methods named
`_Empty_Unknown`, `_NotEmpty_Known`, `__Exercise`, `__Paged`, `__Transactional`, `_<Exception>_<Why>` and the like.
Every class and method carries javadoc saying what it asserts and why the persistence contexts behave as they do.

### `_DataJpaTest`: fixtures are persisted by the test

The H2 starts empty, so each test persists what it needs, through the fixtures, then sets only the attributes the
test is about (random, unique values so the test can count what it finds; the randomizers' codes are lower-case
and their validity periods lie in the 2030s, so nothing collides with an installed row):

```java
final var airport = fixtures().airportData();
airport.setTimezone(timezone);
```

Save through the repository or the fixtures, not `entityManager().persist(...)`, and not
`EntityPersisterUtils.newPersistedInstanceOf(entityManager(), ...)`, which takes the entity manager.

### `_SpringBootIT`: fixtures are existing rows of the actual database

An integration test **reads the sample data; it never fabricates rows**. The expected rows are the ones upstream's
`src/test/sql/<table>_exercises_0N_<level>.sql` files show in their comments, from the `3m` edition of the dump;
assert the first rows and the total where a list is long. A test that must write does so inside `@Transactional`,
so it is rolled back; nothing a test does may outlive it in the demonstration database.

A `Stream`-returning query method has to be consumed inside a transaction the caller opened, and closed
(try-with-resources); outside one, Spring Data throws `InvalidDataAccessApiUsageException` before any query runs.

Logging is set in `src/test/resources/logback-spring.xml`, which includes Boot's `defaults.xml` and
`console-appender.xml` and sets the levels: `org.hibernate.SQL` at DEBUG and `org.hibernate.orm.jdbc.bind` at TRACE
for statements and bind values, this package at DEBUG so a test's `log.debug(...)` prints, podam at INFO. Keep
levels there, not under `logging.level` in `application.yaml`, and do not add a `logback.xml` / `logback-test.xml`,
which Logback would read before Boot, without Boot's extensions.
