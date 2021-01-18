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
package ca.stellardrift.permissionsex.test;

import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.impl.config.EmptyPlatformConfiguration;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.subject.SubjectType;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Abstract test for test classes wishing to test in cases requiring a permissions manager
 */
public abstract class PermissionsExTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("TestImpl");
    public static final SubjectType<String> SUBJECTS_GROUP = SubjectType.stringIdentBuilder("group").build();
    public static final SubjectType<UUID> SUBJECTS_USER = SubjectType.builder("user", UUID.class)
            .serializedBy(UUID::toString)
            .deserializedBy(UUID::fromString)
            .build();

    public Path tempFolder;

    private PermissionsEx<?> manager;

    @BeforeEach
    public void setUp(final TestInfo info, final @TempDir Path tempFolder) throws PermissionsLoadingException, IOException, PEBKACException {
        this.tempFolder = tempFolder;
        final PermissionsExConfiguration<EmptyPlatformConfiguration> config = populate();
        config.validate();

        final PermissionsEx<EmptyPlatformConfiguration> manager = new PermissionsEx<>(
            LOGGER,
            tempFolder.resolve(info.getDisplayName()),
            Runnable::run,
            url -> {
                if (url.startsWith("jdbc:h2")) {
                    JdbcDataSource ds = new JdbcDataSource();
                    ds.setURL(url);
                    return ds;
                } else if (url.startsWith("jdbc:mysql")) {
                    return new MariaDbDataSource(url);
                } else if (url.startsWith("jdbc:postgresql")) {
                    PGSimpleDataSource ds = new PGSimpleDataSource();
                    ds.setUrl(url);
                    return ds;
                }
                throw new IllegalArgumentException("Unsupported database implementation!");
            }
        );

        manager.initialize(config);
        this.manager = manager;

        // Register subject types
        this.manager.subjects(SUBJECTS_GROUP);
        this.manager.subjects(SUBJECTS_USER);
    }

    @AfterEach
    public void tearDown() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    protected DataStore dataStore() {
        return this.manager.activeDataStore();
    }

    protected PermissionsEx<?> manager() {
        return manager;
    }

    protected abstract PermissionsExConfiguration<EmptyPlatformConfiguration> populate();
}
