/**
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
package ninja.leaping.permissionsex.backend.file;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.TransformAction;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.logging.TranslatableLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ninja.leaping.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;
import static ninja.leaping.permissionsex.util.Translations.t;

public class SchemaMigrations {
    public static final int LATEST_VERSION = 5;
    private SchemaMigrations() {
    }

    private static ConfigurationTransformation.Builder tBuilder() {
        return ConfigurationTransformation.builder();
    }


    static ConfigurationTransformation versionedMigration(final TranslatableLogger logger) {
        return ConfigurationTransformation.versionedBuilder()
                .setVersionKey("schema-version")
                .addVersion(LATEST_VERSION, fourToFive())
                .addVersion(4, threeToFour())
                .addVersion(3, twoTo3())
                .addVersion(2, oneTo2(logger))
                .addVersion(1, initialTo1())
                .build();
    }

    static ConfigurationTransformation fourToFive() {
        return tBuilder()
                .addAction(new Object[] {"subjects", WILDCARD_OBJECT, WILDCARD_OBJECT}, ((inputPath, valueAtPath) -> {
                    Map<Integer, ConfigurationNode> weightSections = new HashMap<>();
                    return null;
                }))
                .build();
    }

    static ConfigurationTransformation threeToFour() {
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(new Object[]{"worlds", WILDCARD_OBJECT, "inheritance"}, (inputPath, valueAtPath) -> {
                            try {
                                valueAtPath.setValue(Lists.transform(valueAtPath.getList(TypeToken.of(String.class)), input -> "world:" + input));
                            } catch (ObjectMappingException e) {
                                throw new RuntimeException(e);
                            }

                            return new Object[]{"context-inheritance", "world:" + inputPath.get(1)};
                        }).build(),
                tBuilder()
                        .addAction(new Object[]{"worlds"}, (inputPath, valueAtPath) -> {
                            valueAtPath.setValue(null);
                            return null;
                        }).build());
    }

    static ConfigurationTransformation twoTo3() {
        final Map<String, List<Map.Entry<String, Integer>>> convertedRanks = new HashMap<>();
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(new Object[]{WILDCARD_OBJECT}, (nodePath, configurationNode) -> {
                            if (configurationNode.hasMapChildren()) {
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
                        .addAction(new Object[]{"subjects", "group", WILDCARD_OBJECT}, (nodePath, configurationNode) -> {
                            for (ConfigurationNode child : configurationNode.getChildrenList()) {
                                if (child.getNode(FileSubjectData.KEY_CONTEXTS).isVirtual() || child.getNode(FileSubjectData.KEY_CONTEXTS).getChildrenMap().isEmpty()) {
                                    ConfigurationNode optionsNode = child.getNode("options");
                                    if (optionsNode.isVirtual()) {
                                        return null;
                                    }
                                    ConfigurationNode rank = optionsNode.getNode("rank");
                                    if (!rank.isVirtual()) {
                                        final String rankLadder = optionsNode.getNode("rank-ladder").getString("default");
                                        List<Map.Entry<String, Integer>> tempVals = convertedRanks.get(rankLadder.toLowerCase());
                                        if (tempVals == null) {
                                            tempVals = new ArrayList<>();
                                            convertedRanks.put(rankLadder.toLowerCase(), tempVals);
                                        }
                                        tempVals.add(Maps.immutableEntry(configurationNode.getKey().toString(), rank.getInt()));
                                        rank.setValue(null);
                                        optionsNode.getNode("rank-ladder").setValue(null);
                                        if (optionsNode.getChildrenMap().isEmpty()) {
                                            optionsNode.setValue(null);
                                        }
                                    }

                                }
                            }
                            return null;
                        }).build(),
                tBuilder().addAction(new Object[0], (nodePath, configurationNode) -> {
                    for (Map.Entry<String, List<Map.Entry<String, Integer>>> ent : convertedRanks.entrySet()) {
                        Collections.sort(ent.getValue(), (a, b) -> b.getValue().compareTo(a.getValue()));
                        ConfigurationNode ladderNode = configurationNode.getNode(FileDataStore.KEY_RANK_LADDERS, ent.getKey());
                        for (Map.Entry<String, Integer> grp : ent.getValue()) {
                            ladderNode.getAppendedNode().setValue("group:" + grp.getKey());
                        }
                    }
                    return null;
                }).build());
    }

    static ConfigurationTransformation oneTo2(final TranslatableLogger logger) {
        return ConfigurationTransformation.chain(
                tBuilder()
                        .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT}, (nodePath, configurationNode) -> {
                            Object value = configurationNode.getValue();
                            configurationNode.setValue(null);
                            configurationNode.getAppendedNode().setValue(value);
                            return null;
                        })
                        .build(),
                tBuilder()
                        .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, 0, "worlds"}, (nodePath, configurationNode) -> {
                            ConfigurationNode entityNode = configurationNode.getParent().getParent();
                            for (Map.Entry<Object, ? extends ConfigurationNode> ent : configurationNode.getChildrenMap().entrySet()) {
                                entityNode.getAppendedNode().setValue(ent.getValue())
                                        .getNode(FileSubjectData.KEY_CONTEXTS, "world").setValue(ent.getKey());

                            }
                            configurationNode.setValue(null);
                            return null;
                        }).build(),
                tBuilder()
                        .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, WILDCARD_OBJECT, "permissions"}, (nodePath, configurationNode) -> {
                            List<String> existing = configurationNode.getList(Object::toString);
                            if (!existing.isEmpty()) {
                                configurationNode.setValue(ImmutableMap.of());
                            }
                            for (String permission : existing) {
                                int value = permission.startsWith("-") ? -1 : 1;
                                if (value < 0) {
                                    permission = permission.substring(1);
                                }
                                if (permission.equals("*")) {
                                    configurationNode.getParent().getNode("permissions-default").setValue(value);
                                    continue;
                                }
                                permission = ConversionUtils.convertLegacyPermission(permission);
                                if (permission.contains("*")) {
                                    logger.warn(t("The permission at %s contains a now-illegal character '*'", Arrays.toString(configurationNode.getPath())));
                                }
                                configurationNode.getNode(permission).setValue(value);
                            }
                            if (configurationNode.getChildrenMap().isEmpty()) {
                                configurationNode.setValue(null);
                            }
                            return null;
                        })
                        .addAction(new Object[]{"users", WILDCARD_OBJECT, WILDCARD_OBJECT, "group"}, (nodePath, configurationNode) -> {
                            Object[] retPath = nodePath.getArray();
                            retPath[retPath.length - 1] = "parents";
                            for (ConfigurationNode child : configurationNode.getChildrenList()) {
                                child.setValue("group:" + child.getValue());
                            }
                            return retPath;
                        })
                        .addAction(new Object[]{"groups", WILDCARD_OBJECT, WILDCARD_OBJECT, "inheritance"}, (nodePath, configurationNode) -> {
                            Object[] retPath = nodePath.getArray();
                            retPath[retPath.length - 1] = "parents";
                            for (ConfigurationNode child : configurationNode.getChildrenList()) {
                                child.setValue("group:" + child.getValue());
                            }
                            return retPath;
                        })
                        .addAction(new Object[]{"groups", WILDCARD_OBJECT, WILDCARD_OBJECT}, (inputPath, valueAtPath) -> {
                            ConfigurationNode defaultNode = valueAtPath.getNode("options", "default");
                            if (!defaultNode.isVirtual()) {
                                if (defaultNode.getBoolean()) {
                                    ConfigurationNode addToNode = null;
                                    final ConfigurationNode defaultsParent = valueAtPath.getParent().getParent().getParent().getNode("systems", "default");
                                    for (ConfigurationNode node : defaultsParent.getChildrenList()) {
                                        if (Objects.equal(node.getNode(FileSubjectData.KEY_CONTEXTS).getValue(), valueAtPath.getNode(FileSubjectData.KEY_CONTEXTS).getValue())) {
                                            addToNode = node;
                                            break;
                                        }
                                    }
                                    if (addToNode == null) {
                                        addToNode = defaultsParent.getAppendedNode();
                                    }
                                    addToNode.getNode("parents").getAppendedNode().setValue("group:" + valueAtPath.getParent().getKey());
                                }
                                defaultNode.setValue(null);
                                final ConfigurationNode optionsNode = valueAtPath.getNode("options");
                                if (optionsNode.getChildrenMap().isEmpty()) {
                                    optionsNode.setValue(null);
                                }
                            }
                            return null;
                        }).build());
    }

    private static final TransformAction MOVE_PREFIX_SUFFIX_ACTION = (nodePath, configurationNode) -> {
        final ConfigurationNode prefixNode = configurationNode.getNode("prefix");
        if (!prefixNode.isVirtual()) {
            configurationNode.getNode("options", "prefix").setValue(prefixNode);
            prefixNode.setValue(null);
        }

        final ConfigurationNode suffixNode = configurationNode.getNode("suffix");
        if (!suffixNode.isVirtual()) {
            configurationNode.getNode("options", "suffix").setValue(suffixNode);
            suffixNode.setValue(null);
        }

        final ConfigurationNode defaultNode = configurationNode.getNode("default");
        if (!defaultNode.isVirtual()) {
            configurationNode.getNode("options", "default").setValue(defaultNode);
            defaultNode.setValue(null);
        }
        return null;
    };

    static ConfigurationTransformation initialTo1() {
        return ConfigurationTransformation.builder()
                .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT}, MOVE_PREFIX_SUFFIX_ACTION)
                .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, "worlds", WILDCARD_OBJECT}, MOVE_PREFIX_SUFFIX_ACTION)
                .build();
    }
}
