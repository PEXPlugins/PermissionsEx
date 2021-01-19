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
package ca.stellardrift.permissionsex.fabric.impl.bridge;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.fabric.FabricPermissionsEx;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;

import java.util.Set;

public interface PermissionCommandSourceBridge<I> {
    default boolean hasPermission(final String perm) {
        return asCalculatedSubject().hasPermission(perm);
    }

    default CalculatedSubject asCalculatedSubject() {
        return FabricPermissionsEx.engine().subjects(this.permType()).get(this.permIdentifier()).join();
    }

    default Set<ContextValue<?>> activeContexts() {
        return asCalculatedSubject().activeContexts();
    }

    /**
     * Get a reference pointing to this subject.
     */
    default SubjectRef<I> asReference() {
        return SubjectRef.subject(this.permType(), this.permIdentifier());
    }

    SubjectType<I> permType();
    I permIdentifier();

}
