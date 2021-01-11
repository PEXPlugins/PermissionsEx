/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.permissionsex.datastore;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.subject.SubjectRef;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

/**
 * Access to internals exposed for data store use only.
 *
 * @since 2.0.0
 */
public interface DataStoreContext {

    /**
     * Get the permissions engine this context is attached to.
     *
     * @return the engine
     */
    PermissionsEngine engine();

    /**
     * Deserialize a subject reference given a type and identifier.
     *
     * @param pair the subject type to identifier pair
     * @return a resolved subject ref
     */
    default SubjectRef<?> deserializeSubjectRef(final Map.Entry<String, String> pair) {
        return this.deserializeSubjectRef(pair.getKey(), pair.getValue());
    }

    /**
     * Deserialize a subject reference given a type and identifier.
     *
     * @param type the subject type
     * @param identifier the subject identifier
     * @return a resolved subject ref
     */
    SubjectRef<?> deserializeSubjectRef(final String type, final String identifier);

    /**
     * Create a subject ref that will only be resolved once data is queried.
     *
     * @param type the subject type
     * @param identifier the identifier
     * @return a lazy subject reference
     */
    SubjectRef<?> lazySubjectRef(final String type, final String identifier);

    /**
     * Temporary -- create a pooled SQL datasource for a certain URL.
     *
     * @param url the URL to query
     * @return a valid data source
     * @throws SQLException if the connection is invalid
     * @since 2.0.0
     * @deprecated need to find a better place to put this
     */
    @Deprecated
    DataSource dataSourceForUrl(String url) throws SQLException;
}
