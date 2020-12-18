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
package ca.stellardrift.permissionsex.backend.file;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.backend.ConversionUtils;
import ca.stellardrift.permissionsex.backend.Messages;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ScopedConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.spongepowered.configurate.NodePath.path;
import static org.spongepowered.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;
import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

public class SchemaMigrations {
    public static final int LATEST_VERSION = 4;
    private SchemaMigrations() {
    }

    private static ConfigurationTransformation.Builder tBuilder() {
        return ConfigurationTransformation.builder();
    }

    static <N extends ScopedConfigurationNode<N>> ConfigurationTransformation versionedMigration(final FormattedLogger logger) {
        return ConfigurationTransformation.versionedBuilder()
                .versionKey("schema-version")
                .addVersion(LATEST_VERSION, threeToFour())
                .addVersion(3, twoTo3())
                .addVersion(2, oneTo2(logger))
                .addVersion(1, initialTo1())
                .build();
    }

    static ConfigurationTransformation threeToFour() {
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(path("worlds", WILDCARD_OBJECT, "inheritance"), (inputPath, valueAtPath) -> {
                            final List<String> items = valueAtPath.getList(String.class);
                            valueAtPath.raw(null);
                            items.stream()
                                    .map(input -> "world:" + input)
                                    .forEach(longer -> valueAtPath.appendListNode().raw(longer));

                            return new Object[]{"context-inheritance", "world:" + inputPath.get(1)};
                        }).build(),
                tBuilder()
                        .addAction(path("worlds"), (inputPath, valueAtPath) -> {
                            valueAtPath.raw(null);
                            return null;
                        }).build());
    }

    static ConfigurationTransformation twoTo3() {
        final Map<String, List<Map.Entry<String, Integer>>> convertedRanks = new HashMap<>();
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(path(WILDCARD_OBJECT), (nodePath, configurationNode) -> {
                            if (configurationNode.isMap()) {
                                String lastPath = nodePath.get(0).toString();
                                if (lastPath.endsWith("s")) {
                                    lastPath = lastPath.substring(0, lastPath.length() - 1);
                                }
                                return new Object[]{"subjects", lastPath};
                            } else {
                                return null;
                            }
                        }).build(),
                tBuilder()
                        .addAction(path("subjects", "group", WILDCARD_OBJECT), (nodePath, configurationNode) -> {
                            for (ConfigurationNode child : configurationNode.childrenList()) {
                                if (child.node(FileSubjectData.KEY_CONTEXTS).virtual() || child.node(FileSubjectData.KEY_CONTEXTS).childrenMap().isEmpty()) {
                                    ConfigurationNode optionsNode = child.node("options");
                                    if (optionsNode.virtual()) {
                                        return null;
                                    }
                                    ConfigurationNode rank = optionsNode.node("rank");
                                    if (!rank.virtual()) {
                                        final String rankLadder = optionsNode.node("rank-ladder").getString("default");
                                        List<Map.Entry<String, Integer>> tempVals = convertedRanks.computeIfAbsent(rankLadder.toLowerCase(), k -> new ArrayList<>());
                                        tempVals.add(immutableMapEntry(configurationNode.key().toString(), rank.getInt()));
                                        rank.raw(null);
                                        optionsNode.node("rank-ladder").raw(null);
                                        if (optionsNode.childrenMap().isEmpty()) {
                                            optionsNode.raw(null);
                                        }
                                    }

                                }
                            }
                            return null;
                        }).build(),
                tBuilder().addAction(path(), (nodePath, configurationNode) -> {
                    for (Map.Entry<String, List<Map.Entry<String, Integer>>> ent : convertedRanks.entrySet()) {
                        ent.getValue().sort((a, b) -> b.getValue().compareTo(a.getValue()));
                        ConfigurationNode ladderNode = configurationNode.node(FileDataStore.KEY_RANK_LADDERS, ent.getKey());
                        for (Map.Entry<String, Integer> grp : ent.getValue()) {
                            ladderNode.appendListNode().set("group:" + grp.getKey());
                        }
                    }
                    return null;
                }).build());
    }

    static ConfigurationTransformation oneTo2(final FormattedLogger logger) {
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(path(WILDCARD_OBJECT, WILDCARD_OBJECT), (nodePath, configurationNode) -> {
                            final ConfigurationNode src = configurationNode.copy();
                            configurationNode.appendListNode().from(src);
                            return null;
                        })
                        .build(),
                tBuilder()
                        .addAction(path(WILDCARD_OBJECT, WILDCARD_OBJECT, 0, "worlds"), (nodePath, configurationNode) -> {
                            ConfigurationNode entityNode = configurationNode.parent().parent();
                            for (Map.Entry<Object, ? extends ConfigurationNode> ent : configurationNode.childrenMap().entrySet()) {
                                entityNode.appendListNode().from(ent.getValue())
                                        .node(FileSubjectData.KEY_CONTEXTS, "world").set(ent.getKey());

                            }
                            configurationNode.raw(null);
                            return null;
                        }).build(),
                tBuilder()
                        .addAction(path(WILDCARD_OBJECT, WILDCARD_OBJECT, WILDCARD_OBJECT, "permissions"), (nodePath, configurationNode) -> {
                            List<String> existing = configurationNode.getList(String.class, Collections.emptyList());
                            if (!existing.isEmpty()) {
                                configurationNode.raw(Collections.emptyMap());
                            }
                            for (String permission : existing) {
                                int value = permission.startsWith("-") ? -1 : 1;
                                if (value < 0) {
                                    permission = permission.substring(1);
                                }
                                if (permission.equals("*")) {
                                    configurationNode.parent().node("permissions-default").set(value);
                                    continue;
                                }
                                permission = ConversionUtils.convertLegacyPermission(permission);
                                if (permission.contains("*")) {
                                    logger.warn(Messages.FILE_CONVERSION_ILLEGAL_CHAR.toComponent(configurationNode.path()));
                                }
                                configurationNode.node(permission).raw(value);
                            }
                            if (configurationNode.empty()) {
                                configurationNode.raw(null);
                            }
                            return null;
                        })
                        .addAction(path("users", WILDCARD_OBJECT, WILDCARD_OBJECT, "group"), (nodePath, configurationNode) -> {
                            Object[] retPath = nodePath.array();
                            retPath[retPath.length - 1] = "parents";
                            for (ConfigurationNode child : configurationNode.childrenList()) {
                                child.set("group:" + child.getString());
                            }
                            return retPath;
                        })
                        .addAction(path("groups", WILDCARD_OBJECT, WILDCARD_OBJECT, "inheritance"), (nodePath, configurationNode) -> {
                            Object[] retPath = nodePath.array();
                            retPath[retPath.length - 1] = "parents";
                            for (ConfigurationNode child : configurationNode.childrenList()) {
                                child.set("group:" + child.getString());
                            }
                            return retPath;
                        })
                        .addAction(path("groups", WILDCARD_OBJECT, WILDCARD_OBJECT), (inputPath, valueAtPath) -> {
                            final ConfigurationNode defaultNode = valueAtPath.node("options", "default");
                            if (!defaultNode.virtual()) {
                                if (defaultNode.getBoolean()) {
                                    ConfigurationNode addToNode = null;
                                    final ConfigurationNode defaultsParent = valueAtPath.parent().parent().parent().node("fallbacks", PermissionsEngine.SUBJECTS_USER);
                                    final Object contexts = valueAtPath.node(FileSubjectData.KEY_CONTEXTS).raw();
                                    for (ConfigurationNode node : defaultsParent.childrenList()) {
                                        if (Objects.equals(node.node(FileSubjectData.KEY_CONTEXTS).raw(), contexts)) {
                                            addToNode = node;
                                            break;
                                        }
                                    }
                                    if (addToNode == null) {
                                        addToNode = defaultsParent.appendListNode();
                                        addToNode.node(FileSubjectData.KEY_CONTEXTS).set(valueAtPath.node(FileSubjectData.KEY_CONTEXTS));
                                    }

                                    addToNode.node("parents").appendListNode().set("group:" + valueAtPath.parent().key());
                                }
                                defaultNode.raw(null);
                                final ConfigurationNode optionsNode = valueAtPath.node("options");
                                if (optionsNode.childrenMap().isEmpty()) {
                                    optionsNode.raw(null);
                                }
                            }
                            return null;
                        }).build());
    }

    private static final TransformAction MOVE_PREFIX_SUFFIX_ACTION = (nodePath, configurationNode) -> {
        final ConfigurationNode prefixNode = configurationNode.node("prefix");
        if (!prefixNode.virtual()) {
            configurationNode.node("options", "prefix").from(prefixNode);
            prefixNode.set(null);
        }

        final ConfigurationNode suffixNode = configurationNode.node("suffix");
        if (!suffixNode.virtual()) {
            configurationNode.node("options", "suffix").from(suffixNode);
            suffixNode.raw(null);
        }

        final ConfigurationNode defaultNode = configurationNode.node("default");
        if (!defaultNode.virtual()) {
            configurationNode.node("options", "default").from(defaultNode);
            defaultNode.raw(null);
        }
        return null;
    };

    static ConfigurationTransformation initialTo1() {
        return ConfigurationTransformation.builder()
                .addAction(path(WILDCARD_OBJECT, WILDCARD_OBJECT), MOVE_PREFIX_SUFFIX_ACTION)
                .addAction(path(WILDCARD_OBJECT, WILDCARD_OBJECT, "worlds", WILDCARD_OBJECT), MOVE_PREFIX_SUFFIX_ACTION)
                .build();
    }
}
