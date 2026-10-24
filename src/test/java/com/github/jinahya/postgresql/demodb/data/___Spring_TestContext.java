package com.github.jinahya.postgresql.demodb.data;

import com.github.jinahya.postgresql.demodb.persistence.Flight;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * The Spring context the tests of this project boot against.
 * <p>
 * It lives in test sources on purpose: this project publishes repositories, not an application, so nothing in
 * {@code src/main} should carry a {@code @SpringBootApplication}. A slice test such as {@code @DataJpaTest} finds this
 * by walking up from its own package, so tests in this package and below need no {@code classes} attribute.
 * <p>
 * The entities are outside this package, in the sibling package of the main sources, so auto-configuration would
 * not find them; the {@code @EntityScan} points at one of them. The converters they name are found through the
 * mappings ({@code @Convert} on the mapped superclasses), so they need no scan.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = Flight.class)
public class ___Spring_TestContext {

}
