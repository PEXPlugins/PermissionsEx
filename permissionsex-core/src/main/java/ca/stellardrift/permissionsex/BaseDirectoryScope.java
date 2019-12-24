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

package ca.stellardrift.permissionsex;

public enum BaseDirectoryScope {
    /**
     * The base directory for PermissionsEx's configuration files
     */
    CONFIG,
    /**
     * Where jar files should be put to update the plugin
     */
    JAR,
    /**
     * Server base directory, where files like server.properties and ops.json are located
     */
    SERVER,
    /**
     * The server's worlds container
     */
    WORLDS
}
