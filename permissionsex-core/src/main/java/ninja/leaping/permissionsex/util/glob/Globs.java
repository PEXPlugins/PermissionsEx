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
import ninja.leaping.permissionsex.util.glob.parser.GlobLexer;
import ninja.leaping.permissionsex.util.glob.parser.GlobParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class Globs {
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

    public static GlobNode parse(String input) throws GlobParseException {
        if (!(input.contains("{") || input.contains("["))) { // If no special characters, just return raw input
            return new UnitNode(input);
        }
        return parse(new ANTLRInputStream(input));
    }

    private static GlobNode parse(ANTLRInputStream input) throws GlobParseException {
        GlobLexer lexer = new GlobLexer(input);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        GlobParser parser = new GlobParser(tokenStream);
        parser.setErrorHandler(new BailErrorStrategy());
        ParseTreeWalker walker = new ParseTreeWalker();
        GlobListener listener = new GlobListener();

        try {
            walker.walk(listener, parser.rootGlob());
        } catch (ParseCancellationException e) {
            RecognitionException ex = ((RecognitionException) e.getCause());
            throw new GlobParseException("Unable to parse glob: Error at token " + ex.getOffendingToken().getText() + " at position " +  ex.getOffendingToken().getLine() + ":" + ex.getOffendingToken().getCharPositionInLine(), ex);
        }

        return listener.popNode();
    }

    private static List<GlobNode> parseValues(Object[] objs) {
        ImmutableList.Builder<GlobNode> vals = ImmutableList.builder();
        for (Object o : objs) {
            if (o instanceof GlobNode) {
                vals.add((GlobNode) o);
            } else {
                vals.add(literal(String.valueOf(o)));
            }
        }
        return vals.build();
    }
}
