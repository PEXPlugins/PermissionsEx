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

import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;

import java.util.List;
import java.util.Map;

/**
 * A {@link Segment} implementation that blocks all write operations.
 */
public abstract class ReadOnlySegment implements Segment {

    @Override
    public Segment withOption(final String key, final String value) {
        throw readOnly();
    }

    @Override
    public Segment withoutOption(final String key) {
        throw readOnly();
    }

    @Override
    public Segment withOptions(final Map<String, String> values) {
        throw readOnly();
    }

    @Override
    public Segment withPermission(final String permission, final int value) {
        throw readOnly();
    }

    @Override
    public Segment withPermissions(final Map<String, Integer> values) {
        throw readOnly();
    }

    @Override
    public Segment withoutPermissions() {
        throw readOnly();
    }

    @Override
    public <I> Segment plusParent(final SubjectRef<I> ref) {
        throw readOnly();
    }

    @Override
    public <I> Segment minusParent(final SubjectRef<I> ref) {
        throw readOnly();
    }

    @Override
    public Segment withParents(final List<SubjectRef<?>> parents) {
        throw readOnly();
    }

    @Override
    public Segment withoutParents() {
        throw readOnly();
    }

    @Override
    public Segment withFallbackPermission(final int defaultValue) {
        throw readOnly();
    }

    @Override
    public Segment cleared() {
        throw readOnly();
    }

    @Override
    public Segment mergeFrom(final Segment other) {
        throw readOnly();
    }

    protected UnsupportedOperationException readOnly() {
        throw new UnsupportedOperationException("The segment " + this.getClass() + " is read-only!");
    }
}
