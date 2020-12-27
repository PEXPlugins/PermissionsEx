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
package ca.stellardrift.permissionsex.subject;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.util.Change;
import io.leangen.geantyref.TypeToken;
import org.immutables.value.Value;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A reference to a specific subject.
 *
 * @param <I> identifier type
 * @since 2.0.0
 */
@Value.Immutable(builder = false)
public interface SubjectRef<I> {
    TypeToken<SubjectRef<?>> TYPE = new TypeToken<SubjectRef<?>>() {};

    /**
     * Create a new subject reference.
     *
     * @param type the subject type's collection
     * @param identifier the subject's identifier
     * @param <I> the identifier type
     * @return a new subject reference
     * @since 2.0.0
     */
    static <I> SubjectRef<I> subject(final SubjectTypeCollection<I> type, final I identifier) {
        return new SubjectRefImpl<>(type.type(), identifier);
    }

    /**
     * Create a new subject reference.
     *
     * @param type the subject's type
     * @param identifier the subject's identifier
     * @param <I> the identifier type
     * @return a new subject reference
     * @since 2.0.0
     */
    static <I> SubjectRef<I> subject(final SubjectType<I> type, final I identifier) {
        return new SubjectRefImpl<>(type, identifier);
    }

    /**
     * Return a sanitized subject reference that can safely be used as a map key.
     *
     * @param existing the existing reference
     * @param <I> the identifier type
     * @return a sanitized reference
     * @since 2.0.0
     */
    static <I> SubjectRef<I> mapKeySafe(final SubjectRef<I> existing) {
        if (existing instanceof SubjectRefImpl<?>) { // our own immutable reference
            return existing;
        } else {
            return subject(existing.type(), existing.identifier());
        }
    }

    /**
     * The subject's type.
     *
     * @return the type referred to.
     * @since 2.0.0
     */
    @Value.Parameter
    SubjectType<I> type();

    /**
     * An identifier.
     *
     * @return the subject identifier
     * @since 2.0.0
     */
    @Value.Parameter
    I identifier();

    /**
     * Compute the serialized form of the identifier.
     *
     * @return the canonical serialized form of the subject identifier
     */
    default String serializedIdentifier() {
        return this.type().serializeIdentifier(this.identifier());
    }

    /**
     * A resolved reference to a subject's data in a specific collection.
     *
     * @param <I> identifier type
     * @since 2.0.0
     */
    interface ToData<I> extends SubjectRef<I> {
        /**
         * Get the current subject data.
         *
         * @return The current data
         * @since 2.0.0
         */
        ImmutableSubjectData get();

        /**
         * Update the contained data based on the result of a function.
         *
         * @param modifierFunc The function that will be called to update the data
         * @return A future completing when data updates have been written to the data store
         * @since 2.0.0
         */
        CompletableFuture<Change<ImmutableSubjectData>> update(UnaryOperator<ImmutableSubjectData> modifierFunc);

        /**
         * Update a single segment of the contained data based on the provided operator.
         *
         * @param contexts the contexts to update in
         * @param modifierFunc the function that will be called to update the data
         * @return A future completing when data updates have been written to the data store
         * @since 2.0.0
         */
        default CompletableFuture<Change<ImmutableSubjectData>> update(final Set<ContextValue<?>> contexts, UnaryOperator<Segment> modifierFunc) {
            return this.update(data -> data.withSegment(contexts, modifierFunc));
        }

        /**
         * Get whether or not this reference will hold strong references to stored listeners.
         * If the return value  is false, registering a listener object with this reference will
         * not prevent it from being garbage collected, so the listener must be held somewhere
         * else for it to continue being called.
         *
         * @return Whether or not listeners are held strongly.
         * @since 2.0.0
         */
        boolean holdsListenersStrongly();

        /**
         * Register a listener to be called when an update is performed.
         *
         * @param listener The listener to register
         * @since 2.0.0
         */
        void onUpdate(Consumer<ImmutableSubjectData> listener);

        /**
         * Confirm whether or not the subject data referenced is actually registered.
         *
         * @return a future completing with registration state
         * @since 2.0.0
         */
        CompletableFuture<Boolean> isRegistered();

        /**
         * Remove the subject data referenced.
         *
         * @return A future completing with the previous data for this subject.
         * @see SubjectDataCache#remove(Object)
         * @since 2.0.0
         */
        CompletableFuture<ImmutableSubjectData> remove();
    }
}
