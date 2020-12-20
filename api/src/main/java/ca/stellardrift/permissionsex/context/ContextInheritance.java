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
package ca.stellardrift.permissionsex.context;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Holder for information about inheritance between contexts. Immutable.
 *
 * @since 2.0.0
 */
public interface ContextInheritance {
    /**
     * Get the parents of a specific context.
     *
     * <p>When this context is present in a subject's active contexts, its parents are appended
     * to the subject's active contexts for the purpose of data queries.</p>
     *
     * @param context The child context
     * @return Any parent contexts, or an empty list
     * @since 2.0.0
     */
    List<ContextValue<?>> parents(ContextValue<?> context);

    /**
     * Set the parents for a specific context.
     *
     * @param context The context to set parents in
     * @param parents The parents to set, or {@code null} to clear parents for the context.
     * @return A new context inheritance object with the updated parents
     * @since 2.0.0
     */
    ContextInheritance parents(ContextValue<?> context, @Nullable List<ContextValue<?>> parents);

    /**
     * Get all parent data as a map from context to list of parent contexts.
     *
     * <p>The returned map is immutable.</p>
     *
     * @return parents
     * @since 2.0.0
     */
    Map<ContextValue<?>, List<ContextValue<?>>> allParents();

}
