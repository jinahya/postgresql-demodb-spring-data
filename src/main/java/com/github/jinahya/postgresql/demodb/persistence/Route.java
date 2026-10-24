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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedRoute#TABLE_NAME} table, extending {@link MappedRoute}
 * with what is an application's to decide: its associations, their fetch types and their cascades.
 * <p>
 * The {@value MappedRoute#COLUMN_NAME_DAYS_OF_WEEK} and {@value MappedRoute#COLUMN_NAME_DURATION}
 * columns are overridden with a column definition, {@code varchar}, for the in-memory unit's
 * generated schema alone; their converters carry them as text either way.
 * <p>
 * The table has no primary key, so the entity names one of its own: {@link RouteId}, the
 * {@value MappedRoute#COLUMN_NAME_ROUTE_NO} and {@value MappedRoute#COLUMN_NAME_VALIDITY} columns
 * as an {@link EmbeddedId
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 * @EmbeddedId}. The identifier is what writes the two columns, a provider writing an
 *         identifier's columns whatever their {@code insertable} says, so the inherited attributes
 *         over the same columns are overridden to read-only; the two have to agree, see
 *         {@link #setId(RouteId)}.
 */
// the identifier writes route_no and validity -- a provider writes an identifier's columns
// whatever their
// insertable says -- so the inherited attributes over the same two columns are made read-only here
@AttributeOverride(name = MappedRoute.ATTRIBUTE_NAME_ROUTE_NO,
                   column = @Column(name = MappedRoute.COLUMN_NAME_ROUTE_NO,
                                    nullable = false,
                                    insertable = false,
                                    updatable = false))
@AttributeOverride(name = MappedRoute.ATTRIBUTE_NAME_VALIDITY,
                   column = @Column(name = MappedRoute.COLUMN_NAME_VALIDITY,
                                    nullable = false,
                                    insertable = false,
                                    updatable = false))
// the in-memory unit generates its schema from the mappings, and a converter whose database side
// is Object
// gives the generator no type for the column; the overrides supply one. The physical database's
// own DDL is
// what the integration-test unit runs against, so the overrides never reach it.
@AttributeOverride(name = MappedRoute.ATTRIBUTE_NAME_DAYS_OF_WEEK,
                   column = @Column(name = MappedRoute.COLUMN_NAME_DAYS_OF_WEEK,
                                    nullable = false,
                                    columnDefinition = "varchar"))
@AttributeOverride(name = MappedRoute.ATTRIBUTE_NAME_DURATION,
                   column = @Column(name = MappedRoute.COLUMN_NAME_DURATION,
                                    nullable = false,
                                    columnDefinition = "varchar"))
@Entity
@Table(name = MappedRoute.TABLE_NAME)
public class Route
        extends MappedRoute {

    /**
     * Creates a new instance.
     */
    public Route() {
        super();
    }

    /**
     * Replaces the identifier of this route with the specified value, which has to agree with the
     * {@value MappedRoute#COLUMN_NAME_ROUTE_NO} and {@value MappedRoute#COLUMN_NAME_VALIDITY} the
     * inherited attributes hold: the identifier is what is written, the inherited attributes are
     * read-only here.
     *
     * @param id new value for the identifier.
     */
    public void setId(final RouteId id) {
        this.id = id;
    }

    /**
     * Returns the identifier of this route.
     *
     * @return the identifier of this route.
     */
    public RouteId getId() {
        return id;
    }

    /**
     * Returns the airport of departure, over the {@value MappedRoute#COLUMN_NAME_DEPARTURE_AIRPORT}
     * column; the column's writable mapping.
     *
     * @return the departureAirportData.
     */
    public AirportData getDepartureAirportData() {
        return departureAirportData;
    }

    /**
     * Replaces the departureAirportData with the specified value.
     *
     * @param departureAirportData new value for the departureAirportData.
     */
    public void setDepartureAirportData(final AirportData departureAirportData) {
        this.departureAirportData = departureAirportData;
    }

    /**
     * Returns the airport of arrival, over the {@value MappedRoute#COLUMN_NAME_ARRIVAL_AIRPORT}
     * column; the column's writable mapping.
     *
     * @return the arrivalAirportData.
     */
    public AirportData getArrivalAirportData() {
        return arrivalAirportData;
    }

    /**
     * Replaces the arrivalAirportData with the specified value.
     *
     * @param arrivalAirportData new value for the arrivalAirportData.
     */
    public void setArrivalAirportData(final AirportData arrivalAirportData) {
        this.arrivalAirportData = arrivalAirportData;
    }

    /**
     * Returns the airplane which flies the route, over the
     * {@value MappedRoute#COLUMN_NAME_AIRPLANE_CODE} column; the column's writable mapping.
     *
     * @return the airplaneData.
     */
    public AirplaneData getAirplaneData() {
        return airplaneData;
    }

    /**
     * Replaces the airplaneData with the specified value.
     *
     * @param airplaneData new value for the airplaneData.
     */
    public void setAirplaneData(final AirplaneData airplaneData) {
        this.airplaneData = airplaneData;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @EmbeddedId
    private RouteId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_DEPARTURE_AIRPORT, nullable = false)
    private AirportData departureAirportData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_ARRIVAL_AIRPORT, nullable = false)
    private AirportData arrivalAirportData;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_AIRPLANE_CODE, nullable = false)
    private AirplaneData airplaneData;
}
