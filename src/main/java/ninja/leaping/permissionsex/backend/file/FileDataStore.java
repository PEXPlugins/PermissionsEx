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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.json.FieldValueSeparatorStyle;
import ninja.leaping.configurate.json.JSONConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.TransformAction;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static ninja.leaping.configurate.transformation.ConfigurationTransformation.WILDCARD_OBJECT;
import static ninja.leaping.permissionsex.util.Translations._;


public class FileDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("file", FileDataStore.class);

    @Setting
    private String file;
    @Setting
    private boolean compat = false;

    private ConfigurationLoader permissionsFileLoader;
    private ConfigurationNode permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();

    public FileDataStore() {
        super(FACTORY);
    }

    private static ConfigurationTransformation.Builder tBuilder() {
        return ConfigurationTransformation.builder();
    }

    private ConfigurationLoader<? extends ConfigurationNode> createLoader(File file) {
        JSONConfigurationLoader.Builder build = JSONConfigurationLoader.builder()
                .setFile(file)
                .setIndent(4)
                .setFieldValueSeparatorStyle(FieldValueSeparatorStyle.SPACE_AFTER);

        /*if (!compat) {
            build.getFactory().disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
            build.getFactory().enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        }*/
        return build.build();
    }

    private File migrateLegacy(File permissionsFile, String extension, ConfigurationLoader<?> loader, String formatName) throws PermissionsLoadingException {
        File legacyPermissionsFile = permissionsFile;
        file = file.replace(extension, ".json");
        permissionsFile = new File(getManager().getBaseDirectory(), file);
        permissionsFileLoader = createLoader(permissionsFile);
        try {
            permissionsConfig = loader.load();
            permissionsFileLoader.save(permissionsConfig);
            legacyPermissionsFile.renameTo(new File(legacyPermissionsFile.getCanonicalPath() + ".legacy-backup"));
        } catch (IOException e) {
            throw new PermissionsLoadingException(_("While loading legacy %s permissions from %s", formatName, permissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        File permissionsFile = new File(getManager().getBaseDirectory(), file);
        if (file.endsWith(".yml")) {
            permissionsFile = migrateLegacy(permissionsFile, ".yml", YAMLConfigurationLoader.builder().setFile(permissionsFile).build(), "YML");
        } else if (file.endsWith(".conf")) {
            permissionsFile = migrateLegacy(permissionsFile, ".conf", HoconConfigurationLoader.builder().setFile(permissionsFile).build(), "HOCON");
        } else {
            permissionsFileLoader = createLoader(permissionsFile);
        }

        try {
            permissionsConfig = permissionsFileLoader.load(ConfigurationOptions.defaults());//.setMapFactory(MapFactories.unordered()));
        } catch (IOException e) {
            throw new PermissionsLoadingException(_("While loading permissions file from %s", permissionsFile), e);
        }

        final TransformAction movePrefixSuffixDefaultAction = new TransformAction() {
            @Override
            public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
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
            }
        };

        ConfigurationTransformation versionUpdater = ConfigurationTransformation.versionedBuilder()
                .setVersionKey("schema-version")
                .addVersion(2, ConfigurationTransformation.chain(
                        tBuilder()
                                .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
                                        Object value = configurationNode.getValue();
                                        configurationNode.setValue(null);
                                        configurationNode.getAppendedNode().setValue(value);
                                        return null;
                                    }
                                })
                                .build(),
                        tBuilder()
                                .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, 0, "worlds"}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
                                        ConfigurationNode entityNode = configurationNode.getParent().getParent();
                                        for (Map.Entry<Object, ? extends ConfigurationNode> ent : configurationNode.getChildrenMap().entrySet()) {
                                            entityNode.getAppendedNode().setValue(ent.getValue())
                                                    .getNode(FileOptionSubjectData.KEY_CONTEXTS, "world").setValue(ent.getKey());

                                        }
                                        configurationNode.setValue(null);
                                        return null;
                                    }
                                }).build(),
                        tBuilder()
                                .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, WILDCARD_OBJECT, "permissions"}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
                                        List<String> existing = configurationNode.getList(Functions.toStringFunction());
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
                                                getManager().getLogger().warn("The permission at {} contains a now-illegal character '*'", Arrays.toString(configurationNode.getPath()));
                                            }
                                            configurationNode.getNode(permission).setValue(value);
                                        }
                                        return null;
                                    }
                                })
                                .addAction(new Object[]{"users", WILDCARD_OBJECT, WILDCARD_OBJECT, "group"}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
                                        Object[] retPath = nodePath.getArray();
                                        retPath[retPath.length - 1] = "parents";
                                        for (ConfigurationNode child : configurationNode.getChildrenList()) {
                                            child.setValue("group:" + child.getValue());
                                        }
                                        return retPath;
                                    }
                                })
                                .addAction(new Object[]{"groups", WILDCARD_OBJECT, WILDCARD_OBJECT, "inheritance"}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath nodePath, ConfigurationNode configurationNode) {
                                        Object[] retPath = nodePath.getArray();
                                        retPath[retPath.length - 1] = "parents";
                                        for (ConfigurationNode child : configurationNode.getChildrenList()) {
                                            child.setValue("group:" + child.getValue());
                                        }
                                        return retPath;
                                    }
                                })
                                .addAction(new Object[]{"groups", WILDCARD_OBJECT, WILDCARD_OBJECT}, new TransformAction() {
                                    @Override
                                    public Object[] visitPath(ConfigurationTransformation.NodePath inputPath, ConfigurationNode valueAtPath) {
                                        ConfigurationNode defaultNode = valueAtPath.getNode("options", "default");
                                        if (!defaultNode.isVirtual()) {
                                            if (defaultNode.getBoolean()) {
                                                ConfigurationNode addToNode = null;
                                                final ConfigurationNode defaultsParent = valueAtPath.getParent().getParent().getParent().getNode("systems", "default");
                                                for (ConfigurationNode node : defaultsParent.getChildrenList()) {
                                                    if (Objects.equal(node.getNode(FileOptionSubjectData.KEY_CONTEXTS).getValue(), valueAtPath.getNode(FileOptionSubjectData.KEY_CONTEXTS).getValue())) {
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
                                        }
                                        return null;
                                    }
                                }).build()
                ))
                .addVersion(1, ConfigurationTransformation.builder()
                        .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT}, movePrefixSuffixDefaultAction)
                        .addAction(new Object[]{WILDCARD_OBJECT, WILDCARD_OBJECT, "worlds", WILDCARD_OBJECT}, movePrefixSuffixDefaultAction)
                        .build())
                .build();
        int startVersion = permissionsConfig.getNode("schema-version").getInt(-1);
        versionUpdater.apply(permissionsConfig);
        int endVersion = permissionsConfig.getNode("schema-version").getInt();
        if (endVersion > startVersion) {
            getManager().getLogger().info(_("%s schema version updated from %s to %s", permissionsFile, startVersion, endVersion).translateFormatted(Locale.getDefault()));
            try {
                save().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new PermissionsLoadingException(_("While performing version upgrade"), e);
            }
        }
    }

    @Override
    public void close() {

    }

    private ListenableFuture<Void> save() {
        if (saveSuppressed.get() <= 0) {
            final ListenableFutureTask<Void> ret = ListenableFutureTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    saveSync();
                    return null;
                }
            });
            getManager().executeAsyncronously(ret);
            return ret;
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void saveSync() throws IOException {
        if (saveSuppressed.get() <= 0) {
            permissionsFileLoader.save(permissionsConfig);
        }
    }

    private String typeToSection(String type) {
        return type + "s";
    }

    @Override
    public ImmutableOptionSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException {
        try {
            return FileOptionSubjectData.fromNode(permissionsConfig.getNode(typeToSection(type), identifier));
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(_("While deserializing subject data for %s:", identifier), e);
        }
    }

    @Override
    protected ListenableFuture<ImmutableOptionSubjectData> setDataInternal(String type, String identifier, final ImmutableOptionSubjectData data) {
        try {
            if (data == null) {
                permissionsConfig.getNode(typeToSection(type), identifier).setValue(null);
                return Futures.transform(save(), Functions.<ImmutableOptionSubjectData>constant(null));
            }

            final FileOptionSubjectData fileData;

            if (data instanceof FileOptionSubjectData) {
                fileData = (FileOptionSubjectData) data;
            } else {
                fileData = new FileOptionSubjectData();
                ConversionUtils.transfer(data, fileData);
            }
            fileData.serialize(permissionsConfig.getNode(typeToSection(type), identifier));
            return Futures.transform(save(), new Function<Void, ImmutableOptionSubjectData>() {
                @Nullable
                @Override
                public ImmutableOptionSubjectData apply(Void input) {
                    return fileData;
                }
            });
        } catch (ObjectMappingException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public boolean isRegistered(String type, String identifier) {
        return !permissionsConfig.getNode(typeToSection(type), identifier).isVirtual();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<String> getAllIdentifiers(String type) {
        return (Set) this.permissionsConfig.getNode(typeToSection(type)).getChildrenMap().keySet();
    }

    @Override
    public Iterable<String> getRegisteredTypes() {
        return Iterables.transform(Maps.filterValues(this.permissionsConfig.getChildrenMap(), new Predicate<ConfigurationNode>() {
            @Override
            public boolean apply(@Nullable ConfigurationNode input) {
                return input != null && input.hasMapChildren();
            }
        }).keySet(), new Function<Object, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Object input) {
                final String typeStr = input.toString();
                return typeStr.substring(0, typeStr.length() - 1); // trim trailing s
            }
        });
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> getAll() {
        return Iterables.concat(Iterables.transform(permissionsConfig.getChildrenMap().keySet(), new Function<Object, Iterable<Map.Entry<Map.Entry<String,String>,ImmutableOptionSubjectData>>>() {
            @Nullable
            @Override
            public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> apply(@Nullable final Object type) {
                final String typeStr = type.toString();

                return Iterables.transform(getAll(typeStr.substring(0, typeStr.length() - 1)), new Function<Map.Entry<String, ImmutableOptionSubjectData>, Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>>() {
                    @Nullable
                    @Override
                    public Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData> apply(Map.Entry<String, ImmutableOptionSubjectData> input2) {
                        return Maps.immutableEntry(Maps.immutableEntry(type.toString(), input2.getKey()), input2.getValue());
                    }
                });
            }
        }));
    }

    @Override
    protected <T> T performBulkOperationSync(Function<DataStore, T> function) throws Exception {
        saveSuppressed.getAndIncrement();
        T ret;
        try {
            ret = function.apply(this);
        } finally {
            saveSuppressed.getAndDecrement();
        }
        saveSync();
        return ret;
    }
}
