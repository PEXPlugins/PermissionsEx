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

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;

public class SubjectRef implements Map.Entry<String, String> {
    private volatile int id;
    private final String type, identifier;

    SubjectRef(int id, String type, String identifier) {
        this.id = id;
        this.type = type;
        this.identifier = identifier;
    }

    public static SubjectRef unresolved(String type, String name) {
        return new SubjectRef(SqlConstants.UNALLOCATED, type, name);
    }

    public int getId() {
        if (id == SqlConstants.UNALLOCATED) {
            throw new IllegalStateException("Unallocated SubjectRef tried to be used!");
        }
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    boolean isUnallocated() {
        return id == SqlConstants.UNALLOCATED;
    }

    public String getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the key corresponding to this entry.
     *
     * @return the key corresponding to this entry
     * @throws IllegalStateException implementations may, but are not
     *                               required to, throw this exception if the entry has been
     *                               removed from the backing map.
     */
    @Override
    public String getKey() {
        return getType();
    }

    /**
     * Returns the value corresponding to this entry.  If the mapping
     * has been removed from the backing map (by the iterator's
     * <tt>remove</tt> operation), the results of this call are undefined.
     *
     * @return the value corresponding to this entry
     * @throws IllegalStateException implementations may, but are not
     *                               required to, throw this exception if the entry has been
     *                               removed from the backing map.
     */
    @Override
    public String getValue() {
        return getIdentifier();
    }

    /**
     * Replaces the value corresponding to this entry with the specified
     * value (optional operation).  (Writes through to the map.)  The
     * behavior of this call is undefined if the mapping has already been
     * removed from the map (by the iterator's <tt>remove</tt> operation).
     *
     * @param value new value to be stored in this entry
     * @return old value corresponding to the entry
     * @throws UnsupportedOperationException if the <tt>put</tt> operation
     *                                       is not supported by the backing map
     * @throws ClassCastException            if the class of the specified value
     *                                       prevents it from being stored in the backing map
     * @throws NullPointerException          if the backing map does not permit
     *                                       null values, and the specified value is null
     * @throws IllegalArgumentException      if some property of this value
     *                                       prevents it from being stored in the backing map
     * @throws IllegalStateException         implementations may, but are not
     *                                       required to, throw this exception if the entry has been
     *                                       removed from the backing map.
     */
    @Override
    public String setValue(String value) {
        throw new UnsupportedOperationException("Unmodifiable");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectRef)) return false;
        SubjectRef that = (SubjectRef) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("identifier", identifier)
                .toString();
    }
}
