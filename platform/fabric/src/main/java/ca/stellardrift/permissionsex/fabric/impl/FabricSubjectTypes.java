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
package ca.stellardrift.permissionsex.fabric.impl;

import ca.stellardrift.permissionsex.subject.SubjectType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;

import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

public final class FabricSubjectTypes {

    public static final SubjectType<String> SYSTEM = SubjectType.stringIdentBuilder("system")
        .fixedEntries(
            immutableMapEntry("Server", FabricPermissionsExImpl.INSTANCE::server),
            immutableMapEntry(FabricPermissionsExImpl.IDENTIFIER_RCON, () -> null)
        )
        .undefinedValues($ -> true)
        .build();

    // TODO: How can we represent permission level two for command blocks and functions
    public static final SubjectType<String> COMMAND_BLOCK = SubjectType.stringIdentBuilder("command-block")
        .undefinedValues($ -> true)
    .build();

    public static final SubjectType<Identifier> FUNCTION = SubjectType.builder("function", Identifier.class)
        .serializedBy(Identifier::toString)
        .deserializedBy(it -> {
            try {
                return new Identifier(it);
            } catch (final InvalidIdentifierException ex) {
                throw new ca.stellardrift.permissionsex.subject.InvalidIdentifierException(it);
            }
        })
        .undefinedValues($ -> true)
        .build();

    private FabricSubjectTypes() {
    }

}
