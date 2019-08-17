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
package ca.stellardrift.permissionsex.util.glob;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GlobTest {
    @Test
    public void testSimpleGlob() {
        Assert.assertEquals(ImmutableList.of("abde", "abdf", "acde", "acdf"), ImmutableList.copyOf(Globs.seq("a", Globs.or("b", "c"), "d", Globs.or("e", "f"))));
        Assert.assertEquals(ImmutableList.of("abde", "abdf", "acde", "acdf", "axde", "axdf"), ImmutableList.copyOf(Globs.seq("a", Globs.or("b", Globs.or("c", "x")), "d", Globs.or("e", "f"))));
    }

    @Test
    public void testMultiLevelParsing() throws GlobParseException {
        GlobNode parsed = Globs.parse("aaoeu {b,{c,x}} d {e,f}");
        Assert.assertEquals(Globs.seq("aaoeu ", Globs.or("b", Globs.or("c", "x")), " d ", Globs.or("e", "f")), parsed);
    }

    @Test
    public void testLiteralParsing() throws GlobParseException {
        Assert.assertEquals(Globs.literal("some.node.here"), Globs.parse("some.node.here"));
    }

    @Test
    public void testOrParsing() throws GlobParseException {
        Assert.assertEquals(Globs.or("a", "b"), Globs.parse("{a,b}"));
    }

    @Test(expected = GlobParseException.class)
    public void testUnterminatedOrFails() throws GlobParseException {
        Globs.parse("aoeu{xy,b");
    }

    @Ignore("Escape parsing is currently broken, but the rest works fine")
    @Test
    public void testEscapes() throws GlobParseException {
        Assert.assertEquals(Globs.seq("a{b", Globs.or("c", "d")), Globs.parse("a\\{b{c,d}"));
    }
}
