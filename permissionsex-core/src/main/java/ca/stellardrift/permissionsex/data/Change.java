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

package ca.stellardrift.permissionsex.data;

/**
 * Represents a change in the value of some object
 * @param <T> The type of object being changed
 */
public class Change<T> {
    private final T old, newVal;

    public Change(T old, T newVal) {
        this.old = old;
        this.newVal = newVal;
    }

    public T getOld() {
        return this.old;
    }

    public T getNew() {
        return this.newVal;
    }
}
