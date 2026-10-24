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

import com.github.jinahya.postgresql.demodb.persistence.mapped.MappedAirplane;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * An entity which maps the {@value MappedAirplane#TABLE_NAME} view, extending
 * {@link MappedAirplane} with what is an application's to decide; a view has no association to
 * add.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
@Entity
@Table(name = MappedAirplane.TABLE_NAME)
public class Airplane
        extends MappedAirplane {

    /**
     * Creates a new instance.
     */
    public Airplane() {
        super();
    }

    // -----------------------------------------------------------------------------------------------------------------
}
