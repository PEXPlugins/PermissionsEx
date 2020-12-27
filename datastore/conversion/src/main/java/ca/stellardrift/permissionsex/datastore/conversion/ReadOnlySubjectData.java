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
package ca.stellardrift.permissionsex.datastore.conversion;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.Segment;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * An implementation of the subject data interface that blocks all write operations
 */
public abstract class ReadOnlySubjectData implements ImmutableSubjectData {

    @Override
    public ImmutableSubjectData withSegments(BiFunction<Set<ContextValue<?>>, Segment, Segment> transformer) {
        throw readOnly();
    }

    @Override
    public ImmutableSubjectData withSegment(Set<ContextValue<?>> contexts, UnaryOperator<Segment> operation) {
        throw readOnly();
    }

    @Override
    public ImmutableSubjectData withSegment(Set<ContextValue<?>> contexts, Segment segment) {
        throw readOnly();
    }

    @Override
    public ImmutableSubjectData mergeFrom(ImmutableSubjectData other) {
        throw readOnly();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException(this.getClass() + " is a read-only subject data holder, for migration purposes only.");
    }
}
