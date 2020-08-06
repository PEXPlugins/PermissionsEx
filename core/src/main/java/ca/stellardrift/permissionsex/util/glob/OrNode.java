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

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class OrNode extends GlobNode {
    private final List<GlobNode> children;

    public OrNode(List<GlobNode> children) {
        this.children = children;
    }

    @Override
    public Iterator<String> iterator() {
        return Iterators.concat(Iterators.transform(children.iterator(), GlobNode::iterator));
    }

    @Override
    public String toString() {
        return "or(" + children + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrNode)) return false;
        OrNode strings = (OrNode) o;
        return Objects.equals(children, strings.children);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(children);
    }
}
