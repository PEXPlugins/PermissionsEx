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

import ca.stellardrift.permissionsex.util.Change;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface SubjectRef {
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
     * Get an identifier that can be used to refer to this subject.
     *
     * @return The subject's identifier
     * @since 2.0.0
     */
    Map.Entry<String, String> getIdentifier();

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
     * @see SubjectDataCache#remove(String)
     * @since 2.0.0
     */
    CompletableFuture<ImmutableSubjectData> remove();
}
