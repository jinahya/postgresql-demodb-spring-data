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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedSegment;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedSegment#TABLE_NAME} table, extending {@link MappedSegment}
 * with what is an application's to decide: its associations, their fetch types and their cascades.
 * <p>
 * The identifier is the inherited pair of {@code @Id} attributes, named by {@link SegmentId}
 * through {@link IdClass
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 * @IdClass}.
 */
@IdClass(SegmentId.class)
@Entity
@Table(name = MappedSegment.TABLE_NAME)
public class Segment
        extends MappedSegment {

    /**
     * Creates a new instance of the specified ticket and flight, whose identifiers become the two
     * parts of the identifier.
     *
     * @param ticket the ticket.
     * @param flight the flight.
     * @return a new instance.
     */
    public static Segment of(final Ticket ticket, final Flight flight) {
        final var instance = new Segment();
        instance.setTicketNo(ticket.getTicketNo());
        instance.setFlightId(flight.getFlightId());
        instance.ticket = ticket;
        instance.flight = flight;
        return instance;
    }

    /**
     * Creates a new instance.
     */
    public Segment() {
        super();
    }

    /**
     * Returns the ticket this segment is on, over the {@value MappedSegment#COLUMN_NAME_TICKET_NO}
     * column, which is part of the identifier; the association is read-only.
     *
     * @return the ticket.
     */
    public Ticket getTicket() {
        return ticket;
    }

    /**
     * Returns the flight this segment is of, over the {@value MappedSegment#COLUMN_NAME_FLIGHT_ID}
     * column, which is part of the identifier; the association is read-only.
     *
     * @return the flight.
     */
    public Flight getFlight() {
        return flight;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_TICKET_NO, nullable = false, insertable = false,
                updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_FLIGHT_ID, nullable = false, insertable = false,
                updatable = false)
    private Flight flight;
}
