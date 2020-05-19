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

package ca.stellardrift.permissionsex.commands.parse;

import ca.stellardrift.permissionsex.commands.CommonMessages;
import net.kyori.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for converting a quoted string into a list of arguments
 *
 * Grammar is roughly (yeah, this is not really a proper grammar but it gives you an idea of what's happening:
 *
 * WHITESPACE = Character.isWhiteSpace(codePoint)
 * CHAR := (all unicode)
 * ESCAPE := '\' CHAR
 * QUOTE = ' | "
 * UNQUOTED_ARG := (CHAR | ESCAPE)+ WHITESPACE
 * QUOTED_ARG := QUOTE (CHAR | ESCAPE)+ QUOTE
 * ARGS := ((UNQUOTED_ARG | QUOTED_ARG) WHITESPACE+)+
 */
public class QuotedStringParser {
    private final boolean lenient;
    private final String buffer;
    private int index = -1;

    /**
     * Parse from a string of args
     * @param args The raw string of arguments to parse
     * @param lenient Whether to allow unmatched quoted strings, as well as other ambiguous syntax
     * @return A list of argument strings parsed as specified in the pseudo-grammar in the class documentation
     * @throws ArgumentParseException if args are presented with invalid syntax
     */
    public static CommandArgs parseFrom(String args, boolean lenient) throws ArgumentParseException {
        return new QuotedStringParser(args, lenient).parse();
    }

    private QuotedStringParser(String args, boolean lenient) {
        this.buffer = args;
        this.lenient = lenient;
    }

    public CommandArgs parse() throws ArgumentParseException {
        if (buffer.length() == 0) { // Fast track for argless commands
            return new CommandArgs(buffer, Collections.<CommandArgs.SingleArg>emptyList());
        }

        List<CommandArgs.SingleArg> returnedArgs = new ArrayList<>(buffer.length() / 8);
        skipWhiteSpace();
        while (hasMore()) {
            int startIdx = index + 1;
            String arg = nextArg();
            returnedArgs.add(new CommandArgs.SingleArg(arg, startIdx, index));
            skipWhiteSpace();
        }
        return new CommandArgs(buffer, returnedArgs);
    }

    // Utility methods
    private boolean hasMore() {
        return index + 1 < buffer.length();
    }

    private int peek() throws ArgumentParseException {
        if (!hasMore()) {
            throw createException(CommonMessages.ERROR_PARSE_BUFFEROVERRUN.toComponent());
        }
        return buffer.codePointAt(index + 1);
    }

    private int next() throws ArgumentParseException {
        if (!hasMore()) {
            throw createException(CommonMessages.ERROR_PARSE_BUFFEROVERRUN.toComponent());
        }
        return buffer.codePointAt(++index);
    }

    public ArgumentParseException createException(Component message) {
        return new ArgumentParseException(message, buffer, index);
    }

    // Parsing methods

    private void skipWhiteSpace() throws ArgumentParseException {
        if (!hasMore()) {
            return;
        }
        while (Character.isWhitespace(peek())) {
            next();
        }
    }

    private String nextArg() throws ArgumentParseException {
        StringBuilder argBuilder = new StringBuilder();
        int codePoint = peek();
        if (codePoint == '"' || codePoint == '\'') {
            // quoted string
            parseQuotedString(codePoint, argBuilder);
        } else {
            parseUnquotedString(argBuilder);
        }
        return argBuilder.toString();
    }

    private void parseQuotedString(int startQuotation, StringBuilder builder) throws ArgumentParseException {
        // Consume the start quotation character
        int nextCodePoint = next();
        if (nextCodePoint != startQuotation) {
            throw createException(CommonMessages.ERROR_PARSE_NOTQUOTE.toComponent(nextCodePoint, startQuotation));
        }

        while (true) {
            if (!hasMore()) {
                if (lenient) {
                    return;
                } else {
                    throw createException(CommonMessages.ERROR_PARSE_UNTERMINATED_QUOTED.toComponent());
                }
            }
            nextCodePoint = next();
            if (nextCodePoint == startQuotation) {
                return;
            } else if (nextCodePoint == '\\') {
                parseEscape(builder);
            } else {
                builder.appendCodePoint(nextCodePoint);
            }
        }
    }

    private void parseUnquotedString(StringBuilder builder) throws ArgumentParseException {
        while (hasMore()) {
            int nextCodePoint = next();
            if (Character.isWhitespace(nextCodePoint)) {
                return;
            } else if (nextCodePoint == '\\') {
                parseEscape(builder);
            } else {
                builder.appendCodePoint(nextCodePoint);
            }
        }
    }

    private void parseEscape(StringBuilder builder) throws ArgumentParseException {
        builder.appendCodePoint(next()); // TODO: Unicode character escapes (\u00A7 type
    }

}
