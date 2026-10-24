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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirportData;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedAirportData#TABLE_NAME} table, extending
 * {@link MappedAirportData} with what is an application's to decide; it adds nothing, there being
 * no foreign key to map an association over.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
@Entity
@Table(name = MappedAirportData.TABLE_NAME)
public class AirportData
        extends MappedAirportData {

    /**
     * Creates a new instance.
     */
    public AirportData() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * @param airportCode {@inheritDoc}
     * @implNote Widened to {@code public}: the identifier is assigned, not generated, so
     *         whoever creates an instance sets it.
     */
    @Override
    public void setAirportCode(final String airportCode) {
        super.setAirportCode(airportCode);
    }

    // -----------------------------------------------------------------------------------------------------------------
}
