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
import java.util.NoSuchElementException;
import java.util.Objects;

class SequenceNode extends GlobNode {
    private final List<GlobNode> children;

    SequenceNode(List<GlobNode> children) {
        this.children = children;
    }

    @Override
    public Iterator<String> iterator() {
        return new SeqIterator();
    }

    private final class SeqIterator implements Iterator<String> {
        private final Iterator<String>[] iterators;
        private final String[] components;
        private @Nullable String current;

        @SuppressWarnings({"unchecked", "rawtypes"})
        SeqIterator() {
            this.iterators = new Iterator[children.size()];
            this.components = new String[this.iterators.length];
            for (int i = 0; i < children.size(); ++i) {
                this.iterators[i] = children.get(i).iterator();
                if (i != children.size() - 1) {
                    this.components[i] = this.iterators[i].next();
                }
            }
            this.current = computeNext();
        }

        /**
         * This computes the next combination of all strings, stepping through each element sequentially
         *
         * @return The next element
         */
        private @Nullable String computeNext() {
            int lastNum = iterators.length;
            for (int i = iterators.length - 1; i >= 0; --i) {
                lastNum = i;
                if (iterators[i].hasNext()) {
                    break;
                } else if (i == 0) {
                    return null;
                } else {
                    iterators[i] = null;
                }
            }
            for (int i = lastNum; i < iterators.length; ++i) {
                if (iterators[i] == null) {
                    iterators[i] = children.get(i).iterator();
                }
                components[i] = iterators[i].next();
            }
            return String.join("", this.components);
        }

        @Override
        public boolean hasNext() {
            return this.current != null;
        }

        @Override
        public String next() {
            final @Nullable String current = this.current;
            if (current == null) {
                throw new NoSuchElementException();
            }
            this.current = computeNext();
            return current;
        }
    }

    @Override
    public String toString() {
        return "seq(" + this.children + ")";
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof SequenceNode)) return false;
        final SequenceNode that = (SequenceNode) other;
        return Objects.equals(this.children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(children);
    }
}
