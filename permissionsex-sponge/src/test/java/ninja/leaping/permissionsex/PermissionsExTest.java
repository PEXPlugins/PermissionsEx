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
package ninja.leaping.permissionsex;

import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Abstract test for test classes wishing to test in cases requiring a permissions manager
 */
public abstract class PermissionsExTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private PermissionsEx manager;
    @Before
    public void setUp() throws PermissionsLoadingException, ObjectMappingException, IOException, PEBKACException {
        PermissionsExConfiguration config = populate();
        config.validate();

        manager = new PermissionsEx(config, new TestImplementationInterface(tempFolder.newFolder()));
    }

    @After
    public void tearDown() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
    }

    protected PermissionsEx getManager() {
        return manager;
    }

    protected abstract PermissionsExConfiguration populate();
}
