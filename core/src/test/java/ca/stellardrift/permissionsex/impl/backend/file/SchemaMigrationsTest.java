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
package ca.stellardrift.permissionsex.impl.backend.file;

import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import com.google.common.io.Resources;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

public class SchemaMigrationsTest {

    @Test
    void testThreeToFour(@TempDir Path tempDir) throws IOException, ConfigurateException {
        doTest("test3to4.pre.json", "test3to4.post.json", tempDir, SchemaMigrations.threeToFour());
    }

    @Test
    void testTwoToThree(@TempDir Path tempDir) throws IOException, ConfigurateException {
        doTest("test2to3.pre.json", "test2to3.post.json", tempDir, SchemaMigrations.twoTo3());
    }

    @Test
    void testOneToTwo(@TempDir Path tempFolder) throws ConfigurateException, IOException {
        final Path testFile = tempFolder.resolve("test1to2.json");
        final YamlConfigurationLoader yamlLoader = YamlConfigurationLoader.builder()
                .url(getClass().getResource("test1to2.pre.yml"))
                .build();
        final GsonConfigurationLoader jsonSaver = GsonConfigurationLoader.builder()
                .path(testFile)
                .build();

        final ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.oneTo2(WrappingFormattedLogger.of(LoggerFactory.getLogger(getClass()), false)).apply(node);

        jsonSaver.save(node);

        assertLinesMatch(Resources.readLines(getClass().getResource("test1to2.post.json"), UTF_8), Files.readAllLines(testFile, UTF_8));

    }

    @Test
    void testInitialToOne(final @TempDir Path tempFolder) throws ConfigurateException, IOException {
        final Path testFile = tempFolder.resolve("Test0to1.yml");
        final ConfigurationLoader<CommentedConfigurationNode> yamlLoader = YamlConfigurationLoader.builder()
                .path(testFile)
                .url(getClass().getResource("test0to1.pre.yml"))
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        final ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.initialTo1().apply(node);
        yamlLoader.save(node);

        assertLinesMatch(Resources.readLines(getClass().getResource("test0to1.post.yml"), UTF_8), Files.readAllLines(testFile, UTF_8));
    }

    private void doTest(final String preName, final String postName, final Path tempDir, final ConfigurationTransformation xform) throws ConfigurateException, IOException {
        final Path testFile = tempDir.resolve("test.json");
        final ConfigurationLoader<BasicConfigurationNode> jsonLoader = GsonConfigurationLoader.builder()
                .path(testFile)
                .url(getClass().getResource(preName))
                .build();
        ConfigurationNode node = jsonLoader.load();
        xform.apply(node);
        jsonLoader.save(node);

        assertLinesMatch(Resources.readLines(getClass().getResource(postName), UTF_8), Files.readAllLines(testFile, UTF_8));
    }

}
