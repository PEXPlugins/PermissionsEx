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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A repository of context types.
 *
 * @since 2.0.0
 */
public interface ContextDefinitionProvider {
    Set<ContextValue<?>> GLOBAL_CONTEXT = Collections.emptySet();

    CompletableFuture<Set<ContextDefinition<?>>> getUsedContextTypes(); // TODO: part of PermissionsEngine instead?

    /**
     * Register a new context type that can be queried. If there is another context type registered with the same key
     * as the one trying to be registered, the registration will fail.
     *
     * @param contextDefinition The new context type
     * @param <T>               The context value type
     * @return whether the context was successfully registered
     */
    <T> boolean registerContextDefinition(ContextDefinition<T> contextDefinition);

    /**
     * Register multiple context definitions.
     *
     * @param definitions The definitions to register
     * @return The number of definitions that were successfully registered
     * @see #registerContextDefinition for details on how individual registrations occur
     */
    int registerContextDefinitions(ContextDefinition<?>... definitions);

    /**
     * Get an immutable copy as a list of the registered context types
     *
     * @return The registered context types
     */
    List<ContextDefinition<?>> getRegisteredContextTypes();

    default @Nullable ContextDefinition<?> getContextDefinition(final String definitionKey) {
        return getContextDefinition(definitionKey, false);
    }

    @Nullable ContextDefinition<?> getContextDefinition(String definitionKey, boolean allowFallbacks);
}
