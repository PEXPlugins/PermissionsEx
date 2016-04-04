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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

public class SqlDao implements AutoCloseable {
    private final Connection conn;
    private final SqlDataStore ds;

    public SqlDao(SqlDataStore ds) throws SQLException {
        this.ds = ds;
        this.conn = ds.getDataSource().getConnection();
    }

    private PreparedStatement prepareStatement(String query) throws SQLException {
        return conn.prepareStatement(this.ds.insertPrefix(query));
    }


    public Optional<SubjectRef> getSubjectRef(int id) throws SQLException {
        PreparedStatement stmt = prepareStatement("SELECT (type, name) FROM {}subjects WHERE id=?");
        stmt.setInt(1, id);
        ResultSet res = stmt.executeQuery();

        if (!res.next()) {
            return Optional.empty();
        }
        return Optional.of(new SubjectRef(id, res.getString(1), res.getString(2)));
    }

    public Optional<SubjectRef> getSubjectRef(String type, String name) throws SQLException {
        PreparedStatement stmt = prepareStatement("SELECT (type, name) FROM {}subjects WHERE type=? AND name=?");
        stmt.setString(1, type);
        stmt.setString(2, name);
        ResultSet res = stmt.executeQuery();

        if (!res.next()) {
            return Optional.empty();
        }
        return Optional.of(new SubjectRef(res.getInt(1), type, name));
    }

    public boolean removeSubject(SubjectRef ref) throws SQLException {
        PreparedStatement stmt = prepareStatement("DELETE FROM {}subjects WHERE id=?");
        stmt.setInt(1, ref.getId());
        return stmt.executeUpdate() > 0;
    }

    public boolean removeSubject(String type, String name) throws SQLException {
        PreparedStatement stmt = prepareStatement("DELETE FROM {}subjects WHERE type=?, name=?");
        stmt.setString(1, type);
        stmt.setString(2, name);
        return stmt.executeUpdate() > 0;
    }

    public SubjectRef getOrCreateSubjectRef(String type, String name) throws SQLException {
        PreparedStatement stmt = prepareStatement("SELECT (type, name) FROM {}subjects WHERE type=? AND name=?");
        stmt.setString(1, type);
        stmt.setString(2, name);
        ResultSet res = stmt.executeQuery();

        if (res.next()) {
            return new SubjectRef(res.getInt(1), type, name);
        } else {
            stmt = prepareStatement("INSERT INTO {}subjects (type, name) VALUES (?, ?); SELECT LAST_INSERT_ID()");
            stmt.setString(1, type);
            stmt.setString(2, name);

            res = stmt.executeQuery();
            res.next();
            int id = res.getInt(1);
            return new SubjectRef(id, type, name);
        }
    }

    private Set<Entry<String, String>> getSegmentContexts(int segmentId) throws SQLException {
        PreparedStatement stmt = prepareStatement("SELECT (`key`, `value`) FROM {}contexts WHERE segment=?");
        stmt.setInt(1, segmentId);
        ImmutableSet.Builder<Entry<String, String>> res = ImmutableSet.builder();

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            res.add(Maps.immutableEntry(rs.getString(1), rs.getString(2)));
        }
        return res.build();
    }

    public List<Segment> getSegments(SubjectRef ref) throws SQLException {
        // get all segments attached to subject
        PreparedStatement stmt = prepareStatement("SELECT (id, perm_default) FROM {}segments WHERE subject=?");
        stmt.setInt(1, ref.getId());
        ResultSet rs = stmt.executeQuery();
        ImmutableList.Builder<Segment> result = ImmutableList.builder();

        while (rs.next()) {
            final int id = rs.getInt(1);
            Integer permDef = (Integer) rs.getObject(2);
            Set<Entry<String, String>> contexts = getSegmentContexts(id);

            ImmutableMap.Builder<String, Integer> permValues = ImmutableMap.builder();
            ImmutableMap.Builder<String, String> optionValues = ImmutableMap.builder();
            ImmutableList.Builder<SubjectRef> inheritanceValues = ImmutableList.builder();

            stmt = prepareStatement("SELECT (`key`, `value`) FROM {}permissions WHERE segment=?");
            stmt.setInt(1, id);

            ResultSet segmentRs = stmt.executeQuery();
            while (segmentRs.next()) {
                permValues.put(segmentRs.getString(1), segmentRs.getInt(2));
            }

            stmt = prepareStatement("SELECT (`key`, `value`) FROM {}options WHERE segment=?");
            stmt.setInt(1, id);

            segmentRs = stmt.executeQuery();
            while (segmentRs.next()) {
                optionValues.put(segmentRs.getString(1), segmentRs.getString(2));
            }

            stmt = prepareStatement("SELECT * FROM {}inheritance LEFT JOIN ({}subjects) on ({}inheritance.parent={}subjects.id) WHERE segment=?");
            stmt.setInt(1, id);

            segmentRs = stmt.executeQuery();
            while (segmentRs.next()) {
                inheritanceValues.add(new SubjectRef(segmentRs.getInt(3), segmentRs.getString(4), segmentRs.getString(5)));
            }

            result.add(new Segment(id, contexts, permValues.build(), optionValues.build(), inheritanceValues.build(), permDef));

        }
        return result.build();
    }

    public Segment addSegment(SubjectRef ref) throws SQLException {
        PreparedStatement stmt = prepareStatement("INSERT INTO {}segments (subject) VALUES (?); SELECT LAST_INSERT_ID()");
        stmt.setInt(1, ref.getId());

        ResultSet res = stmt.executeQuery();
        res.next();
        return Segment.empty(res.getInt(1));
    }

    public boolean removeSegment(Segment segment) throws SQLException {
        PreparedStatement stmt = prepareStatement("DELETE FROM {}segments WHERE id=?");
        stmt.setInt(1, segment.getId());
        return stmt.executeUpdate() > 0;
    }

    public void updateSegment(Segment segment) throws SQLException {

    }

    public Set<String> getAllIdentifiers(String type) throws SQLException {
        PreparedStatement stmt = prepareStatement("SELECT (name) FROM {}subjects WHERE type=?");
        stmt.setString(1, type);

        ResultSet rs = stmt.executeQuery();
        ImmutableSet.Builder<String> ret = ImmutableSet.builder();

        while (rs.next()) {
            ret.add(rs.getNString(1));
        }


        return ret.build();
    }

    public Set<String> getRegisteredTypes() throws SQLException {
        ResultSet rs = prepareStatement("SELECT DISTINCT (type) FROM {}subjects").executeQuery();
        ImmutableSet.Builder<String> ret = ImmutableSet.builder();

        while (rs.next()) {
            ret.add(rs.getString(1));
        }

        return ret.build();
    }

    @Override
    public void close() throws SQLException {
        this.conn.close();
    }

    public void initializeTables() throws SQLException {

    }

}
