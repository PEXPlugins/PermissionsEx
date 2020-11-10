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

package ca.stellardrift.permissionsex.util.glob;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

final class OrNode extends GlobNode {
    private final List<GlobNode> children;

    OrNode(List<GlobNode> children) {
        this.children = children;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private final Iterator<GlobNode> children = OrNode.this.children.iterator();
            private @Nullable Iterator<String> child;

            @Override
            public boolean hasNext() {
                if (this.child != null && this.child.hasNext()) {
                    return true;
                }

                return this.children.hasNext();
            }

            @Override
            public String next() {
                while (this.child == null) {
                    this.child = this.children.next().iterator();
                    if (!this.child.hasNext()) {
                        this.child = null;
                    }
                }
                final String result = this.child.next();
                if (!this.child.hasNext()) {
                    this.child = null;
                }
                return result;
            }
        };
    }

    @Override
    public String toString() {
        return "or(" + this.children + ")";
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof OrNode)) return false;
        final OrNode that = (OrNode) other;
        return Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.children);
    }
}
