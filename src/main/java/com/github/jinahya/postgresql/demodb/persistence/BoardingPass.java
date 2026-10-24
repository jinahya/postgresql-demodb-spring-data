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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedBoardingPass;
import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedSegment;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedBoardingPass#TABLE_NAME} table, extending
 * {@link MappedBoardingPass} with what is an application's to decide: its associations, their fetch
 * types and their cascades.
 * <p>
 * The identifier is the inherited pair of {@code @Id} attributes, named by {@link BoardingPassId}
 * through {@link IdClass @IdClass}.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
@IdClass(BoardingPassId.class)
@Entity
@Table(name = MappedBoardingPass.TABLE_NAME)
public class BoardingPass
        extends MappedBoardingPass {

    /**
     * Creates a new instance of the specified segment, whose identifier becomes the identifier.
     *
     * @param segment the segment the pass boards.
     * @return a new instance.
     */
    public static BoardingPass of(final Segment segment) {
        final var instance = new BoardingPass();
        instance.setTicketNo(segment.getTicketNo());
        instance.setFlightId(segment.getFlightId());
        instance.segment = segment;
        return instance;
    }

    /**
     * Creates a new instance.
     */
    public BoardingPass() {
        super();
    }

    /**
     * Returns the segment this pass boards, over the
     * {@value MappedBoardingPass#COLUMN_NAME_TICKET_NO} and
     * {@value MappedBoardingPass#COLUMN_NAME_FLIGHT_ID} columns, the identifier; the association is
     * read-only.
     *
     * @return the segment.
     */
    public Segment getSegment() {
        return segment;
    }

    // -----------------------------------------------------------------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = COLUMN_NAME_TICKET_NO,
                        referencedColumnName = MappedSegment.COLUMN_NAME_TICKET_NO,
                        nullable = false,
                        insertable = false,
                        updatable = false
            ),
            @JoinColumn(name = COLUMN_NAME_FLIGHT_ID,
                        referencedColumnName = MappedSegment.COLUMN_NAME_FLIGHT_ID,
                        nullable = false,
                        insertable = false,
                        updatable = false
            )
    })
    private Segment segment;
}
