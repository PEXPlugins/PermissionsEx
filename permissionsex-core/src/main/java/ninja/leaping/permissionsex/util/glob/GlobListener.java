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

import ninja.leaping.permissionsex.util.glob.parser.GlobParser;
import ninja.leaping.permissionsex.util.glob.parser.GlobParserBaseListener;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

class GlobListener extends GlobParserBaseListener {
    private final Deque<List<GlobNode>> children = new ArrayDeque<>();

    public GlobNode popNode() {
        List<GlobNode> children = this.children.pop();
        if (children.size() == 1) {
            return children.get(0);
        } else {
            return new SequenceNode(children);
        }
    }

    @Override
    public void enterGlob(@NotNull GlobParser.GlobContext ctx) {
        super.enterGlob(ctx);
        children.push(newList());
    }

    @Override
    public void exitGlob(@NotNull GlobParser.GlobContext ctx) {
        super.exitGlob(ctx);
        if (children.size() > 1) {
            GlobNode toAdd = popNode();
            children.peek().add(toAdd);
        }
    }

    @Override
    public void enterOr(@NotNull GlobParser.OrContext ctx) {
        super.enterOr(ctx);
        children.push(newList());
    }

    @Override
    public void exitOr(@NotNull GlobParser.OrContext ctx) {
        super.exitOr(ctx);
        GlobNode toAdd = new OrNode(children.pop());
        children.peek().add(toAdd);
    }

    @Override
    public void exitLiteral(@NotNull GlobParser.LiteralContext ctx) {
        super.exitLiteral(ctx);
        children.peek().add(Globs.literal(ctx.getText()));
    }

    private static List<GlobNode> newList() {
        return new ArrayList<>();
    }
}
