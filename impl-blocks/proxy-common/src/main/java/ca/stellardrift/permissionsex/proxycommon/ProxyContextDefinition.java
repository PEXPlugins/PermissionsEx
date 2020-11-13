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

package ca.stellardrift.permissionsex.proxycommon;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.serialize.Scalars;

import java.util.Objects;
import java.util.function.Consumer;

public final class ProxyContextDefinition extends ContextDefinition<Boolean> {
    public static final ProxyContextDefinition INSTANCE = new ProxyContextDefinition();

    private ProxyContextDefinition() {
        super("proxy");
    }

    @Override
    public @NonNull String serialize(final Boolean canonicalValue) {
        return String.valueOf(canonicalValue);
    }

    @Override
    public @Nullable Boolean deserialize(final String userValue) {
        return Scalars.BOOLEAN.tryDeserialize(userValue);
    }

    @Override
    public boolean matches(final Boolean ownVal, final Boolean testVal) {
        return Objects.equals(ownVal, testVal);
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<Boolean> consumer) {
        consumer.accept(true);
    }
}
