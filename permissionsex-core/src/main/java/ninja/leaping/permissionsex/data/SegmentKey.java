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
package ninja.leaping.permissionsex.data;

import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Weighted;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The necessary information to create a key for a segment
 */
public interface SegmentKey extends Weighted {
    Set<Map.Entry<String, String>> DEFAULT_CONTEXTS = PermissionsEx.GLOBAL_CONTEXT;
    int DEFAULT_WEIGHT = 0;
    boolean DEFAULT_INHERITABILITY = true;

    // -- Applicability flags
    Set<Map.Entry<String, String>> getContexts();

    SegmentKey withContexts(Set<Map.Entry<String, String>> contexts);

    SegmentKey withWeight(int weight);

    boolean isInheritable();

    SegmentKey withInheritability(boolean inheritable);

    static SegmentKey of() {
        return of(DEFAULT_CONTEXTS, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY);
    }

    static SegmentKey of(Set<Map.Entry<String, String>> contexts) {
        return of(contexts, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY);
    }

    static SegmentKey of(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable) {
        return new SegmentKeyImpl(contexts, weight, inheritable);
    }

    class SegmentKeyImpl implements SegmentKey {
        private final Set<Map.Entry<String, String>> contexts;
        private final int weight, hashCode;
        private final boolean inheritable;

        public SegmentKeyImpl(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable) {
            this.contexts = ImmutableSet.copyOf(contexts);
            this.weight = weight;
            this.inheritable = inheritable;
            this.hashCode = Objects.hash(this.contexts, this.weight, this.inheritable);
        }

        @Override
        public Set<Map.Entry<String, String>> getContexts() {
            return this.contexts;
        }

        @Override
        public SegmentKey withContexts(Set<Map.Entry<String, String>> contexts) {
            return new SegmentKeyImpl(contexts, this.weight, this.inheritable);
        }

        @Override
        public int getWeight() {
            return this.weight;
        }

        @Override
        public SegmentKey withWeight(int weight) {
            return new SegmentKeyImpl(this.contexts, weight, this.inheritable);
        }

        @Override
        public boolean isInheritable() {
            return this.inheritable;
        }

        @Override
        public SegmentKey withInheritability(boolean inheritable) {
            return new SegmentKeyImpl(this.contexts, this.weight, inheritable);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SegmentKeyImpl)) return false;
            SegmentKeyImpl that = (SegmentKeyImpl) o;
            return weight == that.weight &&
                    inheritable == that.inheritable &&
                    Objects.equals(contexts, that.contexts);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("contexts", contexts)
                    .add("weight", weight)
                    .add("inheritable", inheritable)
                    .toString();
        }
    }
}
