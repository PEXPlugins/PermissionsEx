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
package ca.stellardrift.permissionsex.impl.subject;

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import com.google.errorprone.annotations.concurrent.LazyInit;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LazySubjectRef implements SubjectRef<Object> {
    private final PermissionsEx<?> pex;
    private final String type;
    private final String ident;
    private @LazyInit @MonotonicNonNull SubjectRef<?> resolved;

    public LazySubjectRef(PermissionsEx<?> pex, String type, String ident) {
        this.pex = pex;
        this.type = type;
        this.ident = ident;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SubjectType<Object> type() {
        return (SubjectType<Object>) this.resolved().type();
    }

    @Override
    public Object identifier() {
        return this.resolved().serializedIdentifier();
    }

    public String serializedType() {
        return this.type;
    }

    @Override
    public String serializedIdentifier() {
        final @Nullable SubjectRef<?> resolved = this.resolved;
        if (resolved != null) {
            return resolved.serializedIdentifier();
        } else {
            return this.ident;
        }
    }

    public SubjectRef<?> resolved() {
        if (this.resolved != null) {
            return this.resolved;
        }
        return this.resolved = this.pex.deserializeSubjectRef(this.type, this.ident);
    }
}
