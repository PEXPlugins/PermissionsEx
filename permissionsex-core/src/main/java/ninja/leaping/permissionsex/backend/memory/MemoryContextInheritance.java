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
package ninja.leaping.permissionsex.backend.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context inheritance data structure
 */
@ConfigSerializable
public class MemoryContextInheritance implements ContextInheritance {
    @Setting("context-inheritance")
    private Map<String, List<String>> contextInheritance = new HashMap<>();

    protected MemoryContextInheritance() {

    }

    protected MemoryContextInheritance(Map<String, List<String>> data) {
        this.contextInheritance = data;
    }

    @Override
    public List<Map.Entry<String, String>> getParents(Map.Entry<String, String> context) {
        final List<String> inheritance = contextInheritance.get(Util.contextToString(context));
        if (inheritance == null) {
            return ImmutableList.of();
        }

        return Collections.unmodifiableList(Lists.transform(inheritance, Util::contextFromString));
    }

    @Override
    public ContextInheritance setParents(Map.Entry<String, String> context, List<Map.Entry<String, String>> parents) {
        final Map<String, List<String>> newData = new HashMap<>(contextInheritance);
        newData.put(Util.contextToString(context), ImmutableList.copyOf(Lists.transform(ImmutableList.copyOf(parents), Util::contextToString)));
        return newCopy(newData);
    }

    @Override
    public Map<Map.Entry<String, String>, List<Map.Entry<String, String>>> getAllParents() {
        ImmutableMap.Builder<Map.Entry<String, String>, List<Map.Entry<String, String>>> ret = ImmutableMap.builder();
        for (Map.Entry<String, List<String>> entry : contextInheritance.entrySet()) {
            ret.put(Util.contextFromString(entry.getKey()), Lists.transform(entry.getValue(), Util::contextFromString));
        }
        return ret.build();
    }

    protected MemoryContextInheritance newCopy(Map<String, List<String>> raw) {
        return new MemoryContextInheritance(raw);
    }

    public static MemoryContextInheritance fromExistingContextInheritance(ContextInheritance inheritance) {
        if (inheritance instanceof MemoryContextInheritance) {
            return ((MemoryContextInheritance) inheritance);
        } else {
            Map<String, List<String>> data = new HashMap<>();
            for (Map.Entry<Map.Entry<String, String>, List<Map.Entry<String, String>>> ent : inheritance.getAllParents().entrySet()) {
                data.put(Util.contextToString(ent.getKey()), Lists.transform(ent.getValue(), Util::contextToString));
            }
            return new MemoryContextInheritance(data);
        }
    }


}
