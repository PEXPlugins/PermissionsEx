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
package ca.stellardrift.permissionsex.fabric.impl.context;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class IdentifierContextDefinition extends ContextDefinition<Identifier> {

    protected IdentifierContextDefinition(final String name) {
        super(name);
    }

    @Override
    public String serialize(final Identifier canonicalValue) {
        return canonicalValue.toString();
    }

    @Override
    public @Nullable Identifier deserialize(final String userValue) {
        return Identifier.tryParse(userValue);
    }

}
