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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedBoardingPassId;

import java.io.Serial;
import java.io.Serializable;

/**
 * The id class of {@link BoardingPass}: {@link MappedBoardingPassId} made concrete and
 * {@link Serializable}, as an {@link jakarta.persistence.IdClass @IdClass} has to be.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
public class BoardingPassId
        extends MappedBoardingPassId
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------------------------------
    // STATIC_FACTORY_METHODS

    /**
     * Creates a new instance of the specified values.
     *
     * @param ticketNo a value for the {@value #ATTRIBUTE_NAME_TICKET_NO} attribute.
     * @param flightId a value for the {@value #ATTRIBUTE_NAME_FLIGHT_ID} attribute.
     * @return a new instance.
     */
    public static BoardingPassId of(final String ticketNo, final Integer flightId) {
        return of(BoardingPassId::new, ticketNo, flightId);
    }

    // ---------------------------------------------------------------------------------------------------- CONSTRUCTORS

    /**
     * Creates a new instance. The constructor is {@code public}, as the specification requires of a
     * primary key class.
     */
    public BoardingPassId() {
        super();
    }
}
