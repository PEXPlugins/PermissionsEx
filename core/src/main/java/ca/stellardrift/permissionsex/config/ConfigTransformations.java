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

import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.MoveStrategy;
import ninja.leaping.configurate.transformation.TransformAction;

import static ninja.leaping.configurate.transformation.ConfigurationTransformation.builder;

public class ConfigTransformations {
    private static final TransformAction DELETE_ITEM = (inputPath, valueAtPath) -> {
        valueAtPath.setValue(null);
        return null;
    };

    /**
     * Creat a configuration transformation that converts the Bukkit/1.x global configuration structure to the new v2.x configuration structure.
     * @return A transformation that converts a 1.x-style configuration to a 2.x-style configuration
     */
    private static ConfigurationTransformation initialToZero() {
        return builder()
                        .setMoveStrategy(MoveStrategy.MERGE)
                        .addAction(p("permissions"), (inputPath, valueAtPath) -> new Object[0])
                        .addAction(p("permissions", "backend"), (inputPath, valueAtPath) -> p("default-backend"))
                        .addAction(p("permissions", "allowOps"), DELETE_ITEM)
                        .addAction(p("permissions", "basedir"), DELETE_ITEM)
                        .addAction(p("updater"), (inputPath, valueAtPath) -> {
                            valueAtPath.getNode("enabled").setValue(valueAtPath.getValue());
                            valueAtPath.getNode("always-update").setValue(valueAtPath.getParent().getNode("alwaysUpdate"));
                            valueAtPath.getParent().getNode("alwaysUpdate").setValue(null);
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
