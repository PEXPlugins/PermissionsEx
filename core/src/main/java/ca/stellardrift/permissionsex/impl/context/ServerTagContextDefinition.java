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
package ca.stellardrift.permissionsex.impl.context;

import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Applies {@code server-tag} context values based on tags defined in the configuration.
 */
public class ServerTagContextDefinition extends PEXContextDefinition<String> {
    public static final ServerTagContextDefinition INSTANCE = new ServerTagContextDefinition();
    private List<String> activeTags = Collections.emptyList();

    private ServerTagContextDefinition() {
        super("server-tag");
    }

    @Override
    public String serialize(final String canonicalValue) {
        return canonicalValue;
    }

    @Override
    public @Nullable String deserialize(final String userValue) {
        return userValue;
    }

    @Override
    public boolean matches(final String ownVal, final String testVal) {
        return Objects.equals(ownVal, testVal);
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
        this.activeTags.forEach(consumer);
    }

    @Override
    public void update(PermissionsExConfiguration<?> config) {
        activeTags = config.getServerTags();
    }
}
