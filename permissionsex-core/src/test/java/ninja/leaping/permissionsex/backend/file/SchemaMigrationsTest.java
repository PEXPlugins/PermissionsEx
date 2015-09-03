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

import com.google.common.io.Files;
import com.google.common.io.Resources;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SchemaMigrationsTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    @Test
    public void testThreeToFour() throws IOException {
        final File testFile = tempFolder.newFile();
        ConfigurationLoader<ConfigurationNode> jsonLoader = GsonConfigurationLoader.builder()
                .setSource(Resources.asCharSource(getClass().getResource("test3to4.pre.json"), StandardCharsets.UTF_8))
                .setSink(Files.asCharSink(testFile, StandardCharsets.UTF_8))
                .build();
        ConfigurationNode node = jsonLoader.load();
        SchemaMigrations.threeToFour().apply(node);
        jsonLoader.save(node);
        assertEquals(Resources.toString(getClass().getResource("test3to4.post.json"), StandardCharsets.UTF_8), Files.toString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testTwoToThree() throws IOException {
        final File testFile = tempFolder.newFile();
        ConfigurationLoader<ConfigurationNode> jsonLoader = GsonConfigurationLoader.builder()
                .setSource(Resources.asCharSource(getClass().getResource("test2to3.pre.json"), StandardCharsets.UTF_8))
                .setSink(Files.asCharSink(testFile, StandardCharsets.UTF_8))
                .build();
        ConfigurationNode node = jsonLoader.load();
        SchemaMigrations.twoTo3().apply(node);
        jsonLoader.save(node);
        assertEquals(Resources.toString(getClass().getResource("test2to3.post.json"), StandardCharsets.UTF_8), Files.toString(testFile, StandardCharsets.UTF_8));

    }

    @Test
    public void testOneToTwo() throws IOException {
        final File testFile = tempFolder.newFile();
        ConfigurationLoader<ConfigurationNode> yamlLoader = YAMLConfigurationLoader.builder()
                .setSource(Resources.asCharSource(getClass().getResource("test1to2.pre.yml"), StandardCharsets.UTF_8))
                .build();
        ConfigurationLoader<ConfigurationNode> jsonSaver = GsonConfigurationLoader.builder()
                .setFile(testFile)
                .build();
        ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.oneTo2(TranslatableLogger.forLogger(LoggerFactory.getLogger(getClass()))).apply(node);
        jsonSaver.save(node);
        assertEquals(Resources.toString(getClass().getResource("test1to2.post.json"), StandardCharsets.UTF_8), Files.toString(testFile, StandardCharsets.UTF_8));

    }

    @Test
    public void testInitialToOne() throws IOException {
        final File testFile = tempFolder.newFile();
        ConfigurationLoader<ConfigurationNode> yamlLoader = YAMLConfigurationLoader.builder()
                .setSource(Resources.asCharSource(getClass().getResource("test0to1.pre.yml"), StandardCharsets.UTF_8))
                .setSink(Files.asCharSink(testFile, StandardCharsets.UTF_8))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .build();
        ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.initialTo1().apply(node);
        yamlLoader.save(node);
        assertEquals(Resources.toString(getClass().getResource("test0to1.post.yml"), StandardCharsets.UTF_8), Files.toString(testFile, StandardCharsets.UTF_8));
    }
}
