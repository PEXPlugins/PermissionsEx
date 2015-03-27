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

/**
 * Created by zml on 20.03.15.
 */
public abstract class Command {

    public <TextType> void process(Commander<TextType> commander, CommandArgs arguments) throws ArgumentParseException {
        parseArguments(commander, arguments);
        execute(commander, arguments);
    }

    public <TextType> String getTabCompletions(Commander<TextType> commander, CommandArgs arguments) {
        try {
            parseArguments(commander, arguments);
        } catch (ArgumentParseException ex) {

        }
        return null;

    }

    protected abstract <TextType> void parseArguments(Commander<TextType> commander, CommandArgs args) throws ArgumentParseException;

    protected abstract <TextType> void execute(Commander<TextType> commander, CommandArgs args);
}
