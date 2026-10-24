package com.github.jinahya.postgresql.demodb.persistence;

/*-
 * #%L
 * postgres-demonstration-persistence
 * %%
 * Copyright (C) 2026 Jinahya, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedRoute;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * The identifier {@link Route} gives the {@value MappedRoute#TABLE_NAME} table, which has none of
 * its own: the {@value MappedRoute#COLUMN_NAME_ROUTE_NO} and
 * {@value MappedRoute#COLUMN_NAME_VALIDITY} columns, the pair the table's exclusion constraint
 * keeps apart. This is the mapping which writes the two columns, since a provider writes an
 * identifier's columns whatever their {@code insertable} says; {@link Route} overrides the
 * inherited attributes over the same columns to read-only.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
@Embeddable
public class RouteId
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------------------------------
    // STATIC_FACTORY_METHODS

    /**
     * Creates a new instance of the specified values.
     *
     * @param routeNo  a value for the {@code routeNo} attribute.
     * @param validity a value for the {@code validity} attribute.
     * @return a new instance.
     */
    public static RouteId of(final String routeNo, final String validity) {
        final var instance = new RouteId();
        instance.setRouteNo(routeNo);
        instance.setValidity(validity);
        return instance;
    }

    // ---------------------------------------------------------------------------------------------------- CONSTRUCTORS

    /**
     * Creates a new instance. The constructor is {@code public}, as the specification requires of a
     * primary key class.
     */
    public RouteId() {
        super();
    }

    // ------------------------------------------------------------------------------------------------ java.lang.Object

    @Override
    public final String toString() {
        return super.toString() + '{' +
               "routeNo=" + routeNo +
               ",validity=" + validity +
               '}';
    }

    @Override
    public final boolean equals(final Object obj) {
        if (!(obj instanceof RouteId that)) {
            return false;
        }
        return Objects.equals(routeNo, that.routeNo)
               && Objects.equals(validity, that.validity);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(routeNo, validity);
    }

    // --------------------------------------------------------------------------------------------------------- routeNo

    /**
     * Returns current value of {@code routeNo} attribute.
     *
     * @return current value of {@code routeNo} attribute.
     */
    public String getRouteNo() {
        return routeNo;
    }

    /**
     * Replaces current value of {@code routeNo} attribute with the specified value.
     *
     * @param routeNo new value for {@code routeNo} attribute.
     */
    protected void setRouteNo(final String routeNo) {
        this.routeNo = routeNo;
    }

    // -------------------------------------------------------------------------------------------------------- validity

    /**
     * Returns current value of {@code validity} attribute.
     *
     * @return current value of {@code validity} attribute.
     */
    public String getValidity() {
        return validity;
    }

    /**
     * Replaces current value of {@code validity} attribute with the specified value.
     *
     * @param validity new value for {@code validity} attribute.
     */
    protected void setValidity(final String validity) {
        this.validity = validity;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @Basic(optional = false)
    @Column(name = MappedRoute.COLUMN_NAME_ROUTE_NO, nullable = false, insertable = true,
            updatable = false)
    private String routeNo;

    // -----------------------------------------------------------------------------------------------------------------
    @Basic(optional = false)
    @Column(name = MappedRoute.COLUMN_NAME_VALIDITY, nullable = false, insertable = true,
            updatable = false)
    private String validity;
}
