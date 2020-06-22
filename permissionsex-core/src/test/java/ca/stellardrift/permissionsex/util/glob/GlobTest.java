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

package ca.stellardrift.permissionsex.util.glob;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GlobTest {
    @Test
    public void testSimpleGlob() {
        assertIterableEquals(ImmutableList.of("abde", "abdf", "acde", "acdf"), ImmutableList.copyOf(Globs.seq("a", Globs.or("b", "c"), "d", Globs.or("e", "f"))));
        assertIterableEquals(ImmutableList.of("abde", "abdf", "acde", "acdf", "axde", "axdf"), ImmutableList.copyOf(Globs.seq("a", Globs.or("b", Globs.or("c", "x")), "d", Globs.or("e", "f"))));
    }

    @Test
    public void testMultiLevelParsing() throws GlobParseException {
        GlobNode parsed = Globs.parse("aaoeu {b,{c,x}} d {e,f}");
        assertEquals(Globs.seq("aaoeu ", Globs.or("b", Globs.or("c", "x")), " d ", Globs.or("e", "f")), parsed);
    }

    @Test
    public void testLiteralParsing() throws GlobParseException {
        assertEquals(Globs.literal("some.node.here"), Globs.parse("some.node.here"));
    }

    @Test
    public void testOrParsing() throws GlobParseException {
        assertEquals(Globs.or("a", "b"), Globs.parse("{a,b}"));
    }

    @Test
    public void testCharsParsing() throws GlobParseException {
        assertEquals(Globs.chars("hbc"), Globs.parse("[hbc]"));
    }

    @Test
    public void testCharsCombinations() throws GlobParseException {
        assertIterableEquals(ImmutableList.of("hat", "bat", "cat"), Globs.parse("[hbc]at"));
    }

    @Test
    public void testUnterminatedOrFails() {
        assertThrows(GlobParseException.class, () -> Globs.parse("aoeu{xy,b"));
    }

    @Test
    public void testEscapes() throws GlobParseException {
        assertEquals(Globs.seq("a{b", Globs.or("c", "d")), Globs.parse("a\\{b{c,d}"));
    }
}
