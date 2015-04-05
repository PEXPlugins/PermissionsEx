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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache of reference to a specific subject by ID
 */
public class SubjectRef {
    private int subjectId;
    private int typeId;
    private String type;
    private String subject;

    private SubjectRef(String type, int typeId, String subject, int subjectId) {
        this.type = type;
        this.typeId = typeId;
        this.subject = subject;
        this.subjectId = subjectId;
    }

    public static SubjectRef forNames(String type, String subject, Connection conn) throws SQLException {
        try (PreparedStatement prep = conn.prepareStatement("SELECT (`id`) FROM ")) {
            ResultSet res = prep.executeQuery();
            return new SubjectRef(type, -1, subject, -1);
        }
    }

    public boolean isVirtual() {
        return subjectId == -1;
    }

    public void makeRealIfNecessary() {
        if (subjectId == -1) {

        }
    }

    public int getSubjectId() {
        return subjectId;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getSubject() {
        return subject;
    }

    public String getType() {
        return type;
    }

}
