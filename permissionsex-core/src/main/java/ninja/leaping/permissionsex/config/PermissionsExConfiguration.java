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
package ninja.leaping.permissionsex.config;

import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.exception.PEBKACException;

import java.io.IOException;
import java.util.List;

/**
 * Configuration for PermissionsEx
 */
public interface PermissionsExConfiguration {
    DataStore getDataStore(String name);

    DataStore getDefaultDataStore();

    boolean isDebugEnabled();

    List<String> getServerTags();

    void validate() throws PEBKACException;

    PermissionsExConfiguration reload() throws IOException;

    default void save() throws IOException {}
}
