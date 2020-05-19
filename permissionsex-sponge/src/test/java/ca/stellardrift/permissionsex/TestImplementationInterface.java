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

package ca.stellardrift.permissionsex;

import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import com.google.common.collect.ImmutableSet;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

/**
 */
public class TestImplementationInterface implements ImplementationInterface {
    private final Path baseDirectory;
    private final Logger logger = LoggerFactory.getLogger("TestImpl");

    public TestImplementationInterface(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public Path getBaseDirectory(BaseDirectoryScope scope) {
        return baseDirectory;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public DataSource getDataSourceForURL(String url) {
        return null;
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return Runnable::run;
    }

    @Override
    public void registerCommands(Supplier<Set<CommandSpec>> commandSupplier) {
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
        return ImmutableSet.of();
    }

    @Override
    public String getVersion() {
        return "test";
    }

    @Override
    public CompletableFuture<Integer> lookupMinecraftProfilesByName(Iterable<String> names, Function<MinecraftProfile, CompletableFuture<Void>> action) {
        final int[] count = new int[] {0};
        return CompletableFuture.allOf(StreamSupport.stream(names.spliterator(), false)
                .map(name -> {
                    count[0]++;
                    return action.apply(new TestProfile(name, UUID.nameUUIDFromBytes(name.getBytes(Charsets.UTF_8))));
                })
                .toArray(CompletableFuture[]::new)).thenApply(none -> count[0]);
    }
}

class TestProfile implements MinecraftProfile {
    private final String name;
    private final UUID uid;
    TestProfile(String name, UUID uid) {
        this.name = name;
        this.uid = uid;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @NotNull
    @Override
    public UUID getUuid() {
        return this.uid;
    }
}
