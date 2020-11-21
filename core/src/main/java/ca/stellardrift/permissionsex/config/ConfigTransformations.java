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
package ca.stellardrift.permissionsex.config;

import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.MoveStrategy;
import org.spongepowered.configurate.transformation.TransformAction;

import static org.spongepowered.configurate.NodePath.path;

public final class ConfigTransformations {

    private ConfigTransformations() {
        throw new AssertionError();
    }

    /**
     * Create a configuration transformation that converts the Bukkit/1.x global configuration structure to the new v2.x configuration structure.
     * @return A transformation that converts a 1.x-style configuration to a 2.x-style configuration
     */
    private static ConfigurationTransformation initialToZero() {
        return ConfigurationTransformation.builder()
                        .moveStrategy(MoveStrategy.MERGE)
                        .addAction(path("permissions"), (inputPath, valueAtPath) -> new Object[0])
                        .addAction(path("permissions", "backend"), (inputPath, valueAtPath) -> p("default-backend"))
                        .addAction(path("permissions", "allowOps"), TransformAction.remove())
                        .addAction(path("permissions", "basedir"), TransformAction.remove())
                        .addAction(path("updater"), (inputPath, valueAtPath) -> {
                            valueAtPath.node("enabled").set(valueAtPath.raw());
                            valueAtPath.node("always-update").from(valueAtPath.parent().node("alwaysUpdate"));
                            valueAtPath.parent().node("alwaysUpdate").set(null);
                            return null;
                        })
                        .build();
    }

    public static ConfigurationTransformation versions() {
        return ConfigurationTransformation.versionedBuilder()
                .addVersion(0, initialToZero())
                .build();
    }

    private static Object[] p(Object... path) {
        return path;
    }
}
