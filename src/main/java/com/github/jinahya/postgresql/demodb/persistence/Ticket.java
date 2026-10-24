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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedTicket;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedTicket#TABLE_NAME} table, extending {@link MappedTicket}
 * with what is an application's to decide: its associations, their fetch types and their cascades.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
@Entity
@Table(name = MappedTicket.TABLE_NAME)
public class Ticket
        extends MappedTicket {

    /**
     * Creates a new instance.
     */
    public Ticket() {
        super();
    }

    /**
     * {@inheritDoc}
     *
     * @param ticketNo {@inheritDoc}
     * @implNote Widened to {@code public}: the identifier is assigned, not generated, so
     *         whoever creates an instance sets it.
     */
    @Override
    public void setTicketNo(final String ticketNo) {
        super.setTicketNo(ticketNo);
    }

    /**
     * Returns the booking this ticket belongs to, over the
     * {@value MappedTicket#COLUMN_NAME_BOOK_REF} column; this is the column's writable mapping, the
     * inherited {@code bookRef} being read-only.
     *
     * @return the booking.
     */
    public Booking getBooking() {
        return booking;
    }

    /**
     * Replaces the booking with the specified value.
     *
     * @param booking new value for the booking.
     */
    public void setBooking(final Booking booking) {
        this.booking = booking;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COLUMN_NAME_BOOK_REF, nullable = false)
    private Booking booking;
}
