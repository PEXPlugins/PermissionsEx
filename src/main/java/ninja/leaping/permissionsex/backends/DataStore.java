/**
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
package ninja.leaping.permissionsex.backends;

import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import java.util.Map;

/**
 * Data type abstraction for permissions data
 */
public interface DataStore {
    /**
     * Activate this data store from the required data
     * @throws PermissionsLoadingException If the backing data cannot be loaded
     */
    void initialize(PermissionsEx core) throws PermissionsLoadingException;

    /**
     * Free any resources this backend may be using
     */
    void close();


    ImmutableOptionSubjectData getData(String type, String identifier, Caching listener);

    /**
     * Sets the data
     * @param type The type of subject data is being fetched for
     * @param identifier The identifier of the subject data is being fetched for
     * @param data The data to commit to this backend
     * @return A future that can be used to listen for completion of writing the changed data
     */
    ListenableFuture<ImmutableOptionSubjectData> setData(String type, String identifier, ImmutableOptionSubjectData data);

    boolean isRegistered(String type, String identifier);

    Iterable<Map.Entry<String, ImmutableOptionSubjectData>> getAll(String type);

    Iterable<String> getAllIdentifiers(String type);

    /**
     * Return the type name for this data store
     *
     * @return The type name of this data store
     */
    public String getTypeName();

    /**
     * Serialize the configuration state of this data store to a configuration node
     *
     * @param node The node to serialize state to
     * @return The type name of this data store
     */
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException;
}
