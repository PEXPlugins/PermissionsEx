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
package ninja.leaping.permissionsex.util.command.args;

import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.List;

/**
 * Represents a command argument element.
 */
public abstract class CommandElement {
    private final Translatable key;

    protected CommandElement(Translatable key) {
        this.key = key;
    }

    public Translatable getKey() {
        return this.key;
    }

    /**
     * Parse from an element into children as necessary, or into the provided object.
     *
     * @param res The input parse state
     * @return the output parse state, either the input or any added children
     * @throws ArgumentParseException when a parse error occurs
     */
    public ElementResult parse(ElementResult res)  throws ArgumentParseException {
        Object val = parseValue(res);
        if (this.key != null && val != null) {
            res.addValue(val);
        }
        return res;
    }

    protected abstract Object parseValue(ElementResult args) throws ArgumentParseException;

    public abstract <TextType> List<String> tabComplete(Commander<TextType> src, ElementResult res);

    public <TextType> TextType getUsage(Commander<TextType> src) {
        return src.fmt().tr(getKey());
    }
}
