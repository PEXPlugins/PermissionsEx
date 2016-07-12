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

import com.google.common.io.Resources;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class SchemaMigrationsTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testFourToFive() throws IOException {
        doTest("test4to5.pre.json", "test4to5.post.json", SchemaMigrations.fourToFive());
    }

    @Test
    public void testThreeToFour() throws IOException {
        doTest("test3to4.pre.json", "test3to4.post.json", SchemaMigrations.threeToFour());
    }

    @Test
    public void testTwoToThree() throws IOException {
        doTest("test2to3.pre.json", "test2to3.post.json", SchemaMigrations.twoTo3());
    }

    @Test
    public void testOneToTwo() throws IOException {
        final Path testFile = tempFolder.newFile().toPath();
        ConfigurationLoader<ConfigurationNode> yamlLoader = YAMLConfigurationLoader.builder()
                .setURL(getClass().getResource("test1to2.pre.yml"))
                .build();
        ConfigurationLoader<ConfigurationNode> jsonSaver = GsonConfigurationLoader.builder()
                .setPath(testFile)
                .build();
        ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.oneTo2(TranslatableLogger.forLogger(LoggerFactory.getLogger(getClass()))).apply(node);
        jsonSaver.save(node);
        assertEquals(Resources.readLines(getClass().getResource("test1to2.post.json"), UTF_8), Files.readAllLines(testFile, UTF_8));

    }

    @Test
    public void testInitialToOne() throws IOException {
        final Path testFile = tempFolder.newFile().toPath();
        ConfigurationLoader<ConfigurationNode> yamlLoader = YAMLConfigurationLoader.builder()
                .setPath(testFile)
                .setURL(getClass().getResource("test0to1.pre.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .build();
        ConfigurationNode node = yamlLoader.load();
        SchemaMigrations.initialTo1().apply(node);
        yamlLoader.save(node);
        assertEquals(Resources.readLines(getClass().getResource("test0to1.post.yml"), UTF_8), Files.readAllLines(testFile, UTF_8));
    }

    private void doTest(String preName, String postName, ConfigurationTransformation xform) throws IOException {
        final Path testFile = tempFolder.newFile().toPath();
        ConfigurationLoader<ConfigurationNode> jsonLoader = GsonConfigurationLoader.builder()
                .setPath(testFile)
                .setURL(getClass().getResource(preName))
                .build();
        ConfigurationNode node = jsonLoader.load();
        xform.apply(node);
        jsonLoader.save(node);
        assertEquals(Resources.readLines(getClass().getResource(postName), UTF_8), Files.readAllLines(testFile, UTF_8));

    }
}
