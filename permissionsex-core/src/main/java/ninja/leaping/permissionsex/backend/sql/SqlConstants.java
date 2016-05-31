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
package ninja.leaping.permissionsex.backend.sql;

public class SqlConstants {
    public static final String OPTION_SCHEMA_VERSION = "schema_version";
    public static final int UNALLOCATED = -1;
    /**
     * A version constant representing an unitialized database (no PEX tables, etc)
     */
    public static final int VERSION_NOT_INITIALIZED = -2;
    /**
     * A version constant representing a database in a state matching a version before schema versioning was added
     */
    public static final int VERSION_PRE_VERSIONING = -1;
}
