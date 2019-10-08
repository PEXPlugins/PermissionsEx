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

import com.google.common.base.Joiner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class SequenceNode extends GlobNode {
    private static final Joiner JOIN = Joiner.on("");
    private final List<GlobNode> children;

    public SequenceNode(List<GlobNode> children) {
        Objects.requireNonNull(children, "children");
        this.children = ImmutableList.copyOf(children);
    }

    @Override
    public Iterator<String> iterator() {
        return new SeqIterator();
    }

    private final class SeqIterator extends AbstractIterator<String> {
        private final Iterator<String>[] iterators;
        private final String[] components;

        @SuppressWarnings("unchecked")
        SeqIterator() {
            iterators = new Iterator[children.size()];
            components = new String[iterators.length];
            for (int i = 0; i < children.size(); ++i) {
                iterators[i] = children.get(i).iterator();
                if (i != children.size() - 1) {
                    components[i] = iterators[i].next();
                }
            }
        }

        /**
         * This computes the next combination of all strings, stepping through each element sequentially
         *
         * @return The next element
         */
        @Override
        protected String computeNext() {
            int lastNum = iterators.length;
            for (int i = iterators.length - 1; i >= 0; --i) {
                lastNum = i;
                if (iterators[i].hasNext()) {
                    break;
                } else if (i == 0) {
                    return endOfData();
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

            return JOIN.join(components);
        }
    }

    @Override
    public String toString() {
        return "seq(" + children + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SequenceNode)) return false;
        SequenceNode strings = (SequenceNode) o;
        return Objects.equals(children, strings.children);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(children);
    }
}
