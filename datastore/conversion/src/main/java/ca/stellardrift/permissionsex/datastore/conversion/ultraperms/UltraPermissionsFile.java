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
package ca.stellardrift.permissionsex.datastore.conversion.ultraperms;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

final class UltraPermissionsFile {
    private final ConfigurationLoader<CommentedConfigurationNode> loader;
    private ConfigurationNode node;

    public static void main(final String[] args) throws ConfigurateException {
        final String filename;
        if (args.length > 0) {
            filename = args[0];
        } else {
            System.out.println(Paths.get(".").toAbsolutePath());
            filename = System.console().readLine("File: ");
        }

        final Path path = Paths.get(filename);
        final UltraPermissionsFile upReader = new UltraPermissionsFile(path);
        upReader.unwrapFile();
        upReader.save();
        System.out.println("File " + filename + " unwrapped!");
    }

    UltraPermissionsFile(final Path path) throws ConfigurateException {
        this.loader = YamlConfigurationLoader.builder().path(path).build();
        this.node = this.loader.load();

    }

    void reload() throws ConfigurateException {
        this.node = this.loader.load();
    }

    void save() throws ConfigurateException {
        this.loader.save(this.node);
    }

    private ConfigurationNode getUnwrappedBase64Value(final ConfigurationNode original) {
        if (original.isList() || original.isMap()) {
            return original;
        }

        final @Nullable String raw = original.getString();
        if (raw == null) {
            return original;
        }
        final byte[] data = Base64.getDecoder().decode(raw);

        final GsonConfigurationLoader jsonLoader = GsonConfigurationLoader.builder()
                .defaultOptions(original.options())
            .source(() -> new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)))
                .build();
        try {
            return jsonLoader.load();
        } catch (final ConfigurateException ex) {
            System.out.println("Failed to deserialize entry " + original.key() + ": " + ex.getMessage()); // todo
            ex.printStackTrace();
            return BasicConfigurationNode.root(original.options()).raw(new String(data, StandardCharsets.UTF_8));
        }
    }

    private ConfigurationNode setWrappedBase64Value(final ConfigurationNode destination, final ConfigurationNode value) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(128);
        final GsonConfigurationLoader jsonLoader = GsonConfigurationLoader.builder().
                sink(() -> new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)))
                .build();
        try {
            jsonLoader.save(value);
        } catch (final ConfigurateException ex) {
            throw new RuntimeException(ex); // hopefully this doesn't happen?
        }

        return destination.raw(Base64.getEncoder().encodeToString(out.toByteArray()));
    }

    ConfigurationNode get(final String key) {
        return getUnwrappedBase64Value(node.node(key));
    }

    ConfigurationNode set(final String key, final ConfigurationNode value) {
        return setWrappedBase64Value(node.node(key), value);
    }

    void unwrapFile() {
        if (!this.node.isMap()) {
            return;
        }

        node.childrenMap().forEach((k, v) -> {
                v.from(getUnwrappedBase64Value(v));
        });
    }
}
