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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.serialize.Scalars;

import java.util.Objects;

public final class ProxyContextDefinition extends ContextDefinition<Boolean> {
    public static final ProxyContextDefinition INSTANCE = new ProxyContextDefinition();

    private ProxyContextDefinition() {
        super("proxy");
    }

    @Override
    public @NonNull String serialize(final Boolean userValue) {
        return String.valueOf(userValue);
    }

    @Override
    public @Nullable Boolean deserialize(final String canonicalValue) {
        return Scalars.BOOLEAN.tryDeserialize(canonicalValue);
    }

    @Override
    public boolean matches(final Boolean ownVal, final Boolean testVal) {
        return Objects.equals(ownVal, testVal);
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Function1<? super Boolean, Unit> consumer) {
        consumer.invoke(true);
    }
}
