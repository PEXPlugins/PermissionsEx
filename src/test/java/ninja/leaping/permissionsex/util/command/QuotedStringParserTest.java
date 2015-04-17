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
package ninja.leaping.permissionsex.util.command;

import com.google.common.collect.ImmutableList;
import ninja.leaping.permissionsex.util.command.args.ArgumentParseException;
import ninja.leaping.permissionsex.util.command.args.QuotedStringParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class QuotedStringParserTest {
    private static List<String> parseFrom(String args) throws ArgumentParseException {
        return QuotedStringParser.parseFrom(args, false).getAll(); // Fixed locale for tests
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Test
    public void testEmptyString() throws ArgumentParseException {
        assertEquals(Collections.<String>emptyList(), parseFrom(""));
    }

    @Test
    public void testUnquotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("first", "second", "third"),
                parseFrom("first second third"));
    }

    @Test
    public void testFlagString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("-abc", "value", "something", "--a=b", "--" , "pure", "strings"),
                parseFrom("-abc value something --a=b -- pure strings"));
    }

    @Test
    public void testSingleQuotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("a", "single quoted string", "is", "here"),
                parseFrom("a 'single quoted string' is here"));
    }

    @Test
    public void testDoubleQuotedString() throws ArgumentParseException {
        assertEquals(ImmutableList.of("a", "double quoted string", "is", "here"),
                parseFrom("a \"double quoted string\" is here"));
    }

    @Test
    public void testQuotedParsingDisabled() throws ArgumentParseException {

    }

    @Test
    public void testUnterminatedQuote() throws ArgumentParseException {
        expectedException.expect(ArgumentParseException.class);
        expectedException.expectMessage("Unterminated quoted string");
        parseFrom("a \"unterminated quoted string is bad");
    }

    @Test
    public void testEscape() throws ArgumentParseException {
        assertEquals(ImmutableList.of("this", "demonstrates escapes", "\"of", "various\' characters"),
                parseFrom("this demonstrates\\ escapes \\\"of 'various\\' characters\'"));
    }

}
