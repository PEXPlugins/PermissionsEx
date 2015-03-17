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

import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.PermissionsExTest;
import org.junit.Test;
import org.spongepowered.api.service.permission.context.Context;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for the concept calculator
 */
public class PEXContextCalculatorTest extends PermissionsExTest {
    @Override
    protected void populate(ConfigurationNode node) {
        node.getNode("backends", "test", "type").setValue("memory");
        node.getNode("backends", "test", "something").setValue("nada");
        node.getNode("default-backend").setValue("test");
        ConfigurationNode serverTagsNode = node.getNode("server-tags");
        serverTagsNode.getAppendedNode().setValue("one");
        serverTagsNode.getAppendedNode().setValue("two");
    }

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

}

