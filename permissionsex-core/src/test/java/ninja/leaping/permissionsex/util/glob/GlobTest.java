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
package ninja.leaping.permissionsex.util.glob;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;

import static ninja.leaping.permissionsex.util.glob.Globs.*;
import static org.junit.Assert.*;

public class GlobTest {
    @Test
    public void testSimpleGlob() {
        assertEquals(ImmutableList.of("abde", "abdf", "acde", "acdf"), ImmutableList.copyOf(seq("a", or("b", "c"), "d", or("e", "f"))));
        assertEquals(ImmutableList.of("abde", "abdf", "acde", "acdf", "axde", "axdf"), ImmutableList.copyOf(seq("a", or("b", or("c", "x")), "d", or("e", "f"))));
    }

    @Test
    public void testMultiLevelParsing() throws GlobParseException {
        GlobNode parsed = Globs.parse("aaoeu {b,{c,x}} d {e,f}");
        assertEquals(seq("aaoeu ", or("b", or("c", "x")), " d ", or("e", "f")), parsed);
    }

    @Test
    public void testLiteralParsing() throws GlobParseException {
        assertEquals(literal("some.node.here"), Globs.parse("some.node.here"));
    }

    @Test
    public void testOrParsing() throws GlobParseException {
        assertEquals(or("a", "b"), parse("{a,b}"));
    }

    @Test(expected = GlobParseException.class)
    public void testUnterminatedOrFails() throws GlobParseException {
        parse("aoeu{xy,b");
    }

    @Ignore("Escape parsing is currently broken, but the rest works fine")
    @Test
    public void testEscapes() throws GlobParseException {
        assertEquals(seq("a{b", or("c", "d")), parse("a\\{b{c,d}"));
    }
}
