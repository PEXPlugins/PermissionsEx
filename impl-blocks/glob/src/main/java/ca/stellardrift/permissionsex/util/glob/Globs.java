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

import ca.stellardrift.permissionsex.util.glob.parser.GlobLexer;
import ca.stellardrift.permissionsex.util.glob.parser.GlobParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Globs {
    private Globs() {}

    public static GlobNode or(Object... ors) {
        return new OrNode(parseValues(ors));
    }

    public static GlobNode seq(Object... elements) {
        return new SequenceNode(parseValues(elements));
    }

    public static GlobNode literal(String value) {
        return new UnitNode(value);
    }

    /**
     * Create a new glob node matching any code point in the provided string
     *
     * @param characters Characters to match
     * @return new glob node
     */
    public static GlobNode chars(final String characters) {
        return new CharsNode(characters);
    }

    public static GlobNode parse(String input) throws GlobParseException {
        if (!(input.contains("{") || input.contains("["))) { // If no special characters, just return raw input
            return new UnitNode(input);
        }
        return parse0(CharStreams.fromString(input));
    }

    private static GlobNode parse0(final CharStream input) throws GlobParseException {
        final GlobLexer lexer = new GlobLexer(input);
        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final GlobParser parser = new GlobParser(tokenStream);
        parser.getErrorListeners().clear();
        parser.setErrorHandler(new BailErrorStrategy());

        try {
            return GlobVisitor.INSTANCE.visit(parser.rootGlob());
        } catch (ParseCancellationException e) {
            RecognitionException ex = ((RecognitionException) e.getCause());
            Token errorToken = ex.getOffendingToken();
            throw new GlobParseException(parseErrorMessage(errorToken.getText(),
                    errorToken.getLine(), errorToken.getCharPositionInLine()), ex);
        }
    }

    private static String parseErrorMessage(final String text, final int line, final int column) {
        return "Unable to parse glob: Error at token " + text + " (at position " + line + ":" + column + ")";
    }

    private static List<GlobNode> parseValues(Object[] values) {
        final List<GlobNode> vals = new ArrayList<>(values.length);
        for (final Object value : values) {
            if (value instanceof GlobNode) {
                vals.add((GlobNode) value);
            } else {
                vals.add(literal(String.valueOf(value)));
            }
        }
        return Collections.unmodifiableList(vals);
    }

}
