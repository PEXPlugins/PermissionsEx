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

import ca.stellardrift.permissionsex.util.IpSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract context definiton for context types that use a {@link IpSet}
 */
public abstract class IpSetContextDefinition extends ContextDefinition<IpSet> {

    protected IpSetContextDefinition(final String name) {
        super(name);
    }

    @Override
    public final String serialize(final IpSet canonicalValue) {
        return canonicalValue.toString();
    }

    @Override
    public @Nullable IpSet deserialize(final String userValue) {
        try {
            return IpSet.fromCidr(userValue);
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean matches(final IpSet ownVal, final IpSet testVal) {
        return ownVal.contains(testVal);
    }
}
