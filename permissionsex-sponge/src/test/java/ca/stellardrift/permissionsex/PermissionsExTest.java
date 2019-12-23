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

import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstract test for test classes wishing to test in cases requiring a permissions manager
 */
public abstract class PermissionsExTest {
    private PermissionsEx<?> manager;
    @BeforeEach
    public void setUp(@TempDir Path tempFolder) throws PermissionsLoadingException, ObjectMappingException, IOException, PEBKACException {
        PermissionsExConfiguration<?> config = populate();
        config.validate();

        manager = new PermissionsEx<>(config, new TestImplementationInterface(tempFolder));
    }

    @AfterEach
    public void tearDown() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    protected PermissionsEx<?> getManager() {
        return manager;
    }

    protected abstract PermissionsExConfiguration<?> populate();
}
