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

import org.junit.jupiter.api.Test;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class GlobTest {
    @Test
    void testSimpleGlob() {
        assertIterableEquals(Arrays.asList("abde", "abdf", "acde", "acdf"), Globs.seq("a", Globs.or("b", "c"), "d", Globs.or("e", "f")));
        assertIterableEquals(Arrays.asList("abde", "abdf", "acde", "acdf", "axde", "axdf"), Globs.seq("a", Globs.or("b", Globs.or("c", "x")), "d", Globs.or("e", "f")));
    }

    @Test
    void testMultiLevelParsing() throws GlobParseException {
        GlobNode parsed = Globs.parse("aaoeu {b,{c,x}} d {e,f}");
        assertEquals(Globs.seq("aaoeu ", Globs.or("b", Globs.or("c", "x")), " d ", Globs.or("e", "f")), parsed);
    }

    @Test
    void testLiteralParsing() throws GlobParseException {
        assertEquals(Globs.literal("some.node.here"), Globs.parse("some.node.here"));
    }

    @Test
    void testOrParsing() throws GlobParseException {
        assertEquals(Globs.or("a", "b"), Globs.parse("{a,b}"));
    }

    @Test
    void testCharsParsing() throws GlobParseException {
        assertEquals(Globs.chars("hbc"), Globs.parse("[hbc]"));
    }

    @Test
    void testCharsCombinations() throws GlobParseException {
        assertIterableEquals(Arrays.asList("hat", "bat", "cat"), Globs.parse("[hbc]at"));
    }

    @Test
    void testUnterminatedOrFails() {
        assertThrows(GlobParseException.class, () -> Globs.parse("aoeu{xy,b"));
    }

    @Test
    void testEscapes() throws GlobParseException {
        assertEquals(Globs.seq("a{b", Globs.or("c", "d")), Globs.parse("a\\{b{c,d}"));
    }
}
