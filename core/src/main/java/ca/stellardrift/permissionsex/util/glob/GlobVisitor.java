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

import ca.stellardrift.permissionsex.util.glob.parser.GlobBaseVisitor;
import ca.stellardrift.permissionsex.util.glob.parser.GlobParser;
import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.function.Function;

/* package */ class GlobVisitor extends GlobBaseVisitor<GlobNode> {
    public static final GlobVisitor INSTANCE = new GlobVisitor();

    private GlobVisitor() {}

    @Override
    public GlobNode visitRootGlob(GlobParser.RootGlobContext ctx) {
        return visit(ctx.glob());
    }

    @Override
    public GlobNode visitGlob(GlobParser.GlobContext ctx) {
        if(ctx.element().size() == 1) {
            return visit(ctx.element(0));
        } else {
            return childrenToNode(ctx.element(), SequenceNode::new);
        }
    }

    @Override
    public GlobNode visitTerminal(TerminalNode node) {
        return super.visitTerminal(node);
    }

    @Override
    public GlobNode visitElement(GlobParser.ElementContext ctx) {
        if (ctx.LITERAL() != null) {
            return new UnitNode(unescape(ctx.LITERAL().getText()));
        } else {
            return visitChildren(ctx);
        }
    }

    @Override
    public GlobNode visitChars(GlobParser.CharsContext ctx) {
        return new CharsNode(ctx.content.getText());
    }

    private static final char ESCAPE_MARKER = '\\';

    /**
     * Remove simple escape sequences from a string
     *
     * @param withEscapes input string with escapes
     * @return string with escapes processed
     */
    private static String unescape(final String withEscapes) {
        int escapeIdx = withEscapes.indexOf(ESCAPE_MARKER);
        if (escapeIdx == -1) { // nothing to unescape
            return withEscapes;
        }
        int lastEscape = 0;
        final StringBuilder output = new StringBuilder(withEscapes.length());
        do {
            output.append(withEscapes, lastEscape, escapeIdx);
            lastEscape = escapeIdx + 1;
        } while ((escapeIdx = withEscapes.indexOf(ESCAPE_MARKER, lastEscape + 1)) != -1); // add one extra character to make sure we don't include escaped backslashes
        output.append(withEscapes.substring(lastEscape));
        return output.toString();
    }

    @Override
    public GlobNode visitOr(GlobParser.OrContext ctx) {
        return childrenToNode(ctx.glob(), OrNode::new);
    }

    private <T extends GlobNode> T childrenToNode(final Iterable<? extends ParseTree> contexts, final Function<List<GlobNode>, T> factory) {
        final ImmutableList.Builder<GlobNode> builder = ImmutableList.builder();
        for(ParseTree el : contexts) {
            final GlobNode node = visit(el);
            builder.add(node);
        }
        return factory.apply(builder.build());

    }
}
