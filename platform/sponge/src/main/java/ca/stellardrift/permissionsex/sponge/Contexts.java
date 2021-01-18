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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PSet;
import org.spongepowered.api.service.context.Context;

import java.util.Set;

public final class Contexts {

    private Contexts() {
    }

    public static Set<Context> toSponge(final Set<ContextValue<?>> context)  {
        return PCollections.asSet(context, el -> new Context(el.key(), el.rawValue()));
    }

    public static <T> @Nullable ContextValue<T> toPex(final Context context, final ContextDefinition<T> def) {
        final @Nullable T value = def.deserialize(context.getValue());
        return value == null ? null : def.createValue(value);
    }

    public static Set<ContextValue<?>> toPex(final Set<Context> contexts, final PermissionsEngine manager) {
        PSet<ContextValue<?>> ret = PCollections.set();
        for (final Context ctx : contexts) {
            final @Nullable ContextDefinition<?> def = manager.contextDefinition(ctx.getKey(), true);
            if (def == null) {
                throw new IllegalStateException("A fallback context value was expected!");
            }

            final @Nullable ContextValue<?> ctxVal = toPex(ctx, def);
            if (ctxVal != null) {
                ret = ret.plus(ctxVal);
            }
        }

        return ret;
    }

}
