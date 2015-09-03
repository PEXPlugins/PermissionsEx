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
package ninja.leaping.permissionsex.sponge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.PermissionsExTest;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.exception.PEBKACException;
import org.junit.Test;
import org.spongepowered.api.data.manipulator.immutable.block.ImmutableLayeredData;
import org.spongepowered.api.service.permission.context.Context;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for the concept calculator
 */
public class PEXContextCalculatorTest extends PermissionsExTest {

    @Test
    public void testContextCalculator() {
        PEXContextCalculator calc = new PEXContextCalculator();
        calc.update(getManager().getConfig());
        Set<Context> expected = ImmutableSet.of(new Context(PEXContextCalculator.SERVER_TAG_KEY, "one"),
                new Context(PEXContextCalculator.SERVER_TAG_KEY, "two")), actual = new HashSet<>();
        calc.accumulateContexts(null, actual);
        assertEquals(expected, actual);

        assertTrue(calc.matches(new Context(PEXContextCalculator.SERVER_TAG_KEY, "one"), null));
    }

    @Override
    protected PermissionsExConfiguration populate() {
        return new PermissionsExConfiguration() {
            @Override
            public DataStore getDataStore(String name) {
                return null;
            }

            @Override
            public DataStore getDefaultDataStore() {
                return new MemoryDataStore();
            }

            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public List<String> getServerTags() {
                return ImmutableList.of("one", "two");
            }

            @Override
            public void validate() throws PEBKACException {

            }

            @Override
            public PermissionsExConfiguration reload() throws IOException {
                return this;
            }
        };
    }
}

