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

package ca.stellardrift.permissionsex.backend.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ContextInheritance;

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
    public List<ContextValue<?>> getParents(ContextValue<?> context) {
        final List<String> inheritance = contextInheritance.get(ctxToString(context));
        if (inheritance == null) {
            return ImmutableList.of();
        }

        return Collections.unmodifiableList(Lists.transform(inheritance, MemoryContextInheritance::ctxFromString));
    }

    @Override
    public ContextInheritance setParents(ContextValue<?> context, List<ContextValue<?>> parents) {
        final Map<String, List<String>> newData = new HashMap<>(contextInheritance);
        newData.put(ctxToString(context), ImmutableList.copyOf(Lists.transform(ImmutableList.copyOf(parents), MemoryContextInheritance::ctxToString)));
        return newCopy(newData);
    }

    @Override
    public Map<ContextValue<?>, List<ContextValue<?>>> getAllParents() {
        ImmutableMap.Builder<ContextValue<?>, List<ContextValue<?>>> ret = ImmutableMap.builder();
        for (Map.Entry<String, List<String>> entry : contextInheritance.entrySet()) {
            ret.put(ctxFromString(entry.getKey()), Lists.transform(entry.getValue(), MemoryContextInheritance::ctxFromString));
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
            for (Map.Entry<ContextValue<?>, List<ContextValue<?>>> ent : inheritance.getAllParents().entrySet()) {
                data.put(ctxToString(ent.getKey()), Lists.transform(ent.getValue(), MemoryContextInheritance::ctxToString));
            }
            return new MemoryContextInheritance(data);
        }
    }

    public static ContextValue<?> ctxFromString(String input) {
        final String[] values = input.split(":", 2);
        if (values.length != 2) {
            throw new IllegalArgumentException("Invalid format for context!");
        }
        return new ContextValue<>(values[0], values[1]);
    }

    public static String ctxToString(ContextValue<?> input) {
        return input.getKey() + ":" + input.getRawValue();
    }


}
