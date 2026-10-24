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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedSeat;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedSeat#TABLE_NAME} table, extending {@link MappedSeat} with
 * what is an application's to decide: its associations, their fetch types and their cascades.
 * <p>
 * The identifier is the inherited pair of {@code @Id} attributes, named by {@link SeatId} through
 * {@link IdClass
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 * @IdClass}.
 */
@IdClass(SeatId.class)
@Entity
@Table(name = MappedSeat.TABLE_NAME)
public class Seat
        extends MappedSeat {

    /**
     * Creates a new instance of the specified airplane and seat number.
     *
     * @param airplaneData the airplane the seat is in; its code becomes the
     *                     {@value MappedSeat#COLUMN_NAME_AIRPLANE_CODE} part of the identifier.
     * @param seatNo       the seat number, the other part.
     * @return a new instance.
     */
    public static Seat of(final AirplaneData airplaneData, final String seatNo) {
        final var instance = new Seat();
        instance.setAirplaneCode(airplaneData.getAirplaneCode());
        instance.setSeatNo(seatNo);
        instance.airplaneData = airplaneData;
        return instance;
    }

    /**
     * Creates a new instance.
     */
    public Seat() {
        super();
    }

    /**
     * Returns the airplane this seat is in, over the {@value MappedSeat#COLUMN_NAME_AIRPLANE_CODE}
     * column, which is part of the identifier and so already writable through
     * {@link #setAirplaneCode(String)}; the association is read-only.
     *
     * @return the airplaneData.
     */
    public AirplaneData getAirplaneData() {
        return airplaneData;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_AIRPLANE_CODE, nullable = false, insertable = false,
                updatable = false)
    private AirplaneData airplaneData;
}
