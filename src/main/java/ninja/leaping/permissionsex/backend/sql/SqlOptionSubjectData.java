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

import ninja.leaping.permissionsex.backend.memory.MemoryOptionSubjectData;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Created by zml on 21.03.15.
 */
public class SqlOptionSubjectData extends MemoryOptionSubjectData {
    @Override
    protected MemoryOptionSubjectData newData(Map<Set<Map.Entry<String, String>>, DataEntry> contexts) {
        return new SqlOptionSubjectData(contexts);
    }

    SqlOptionSubjectData() {
        super();
    }

    SqlOptionSubjectData(Map<Set<Map.Entry<String, String>>, DataEntry> contexts) {
        super(contexts);
    }
}
