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
package ninja.leaping.permissionsex.sponge;

import ninja.leaping.permissionsex.util.command.MessageFormatter;
import ninja.leaping.permissionsex.util.command.Commander;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandSource;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * Created by zml on 25.03.15.
 */
public class SpongeCommander implements Commander<Text> {
    private final CommandSource commandSource;
    private final PermissionsExPlugin pex;

    public SpongeCommander(PermissionsExPlugin pex, CommandSource commandSource) {
        this.pex = pex;
        this.commandSource = commandSource;
    }

    @Override
    public String getName() {
        return this.commandSource.getName();
    }

    @Override
    public Set<Map.Entry<String, String>> getActiveContexts() {
        return PEXOptionSubjectData.parSet(commandSource.getActiveContexts());
    }

    @Override
    public MessageFormatter<Text> getResponseElementFactory() {
        return pex.getMessageFormatter();
    }

    @Override
    public void sendMessage(String localizationKey, Object... args) {
        commandSource.sendMessage(Texts.of(MessageFormat.format(localizationKey, args)));
    }

    @Override
    public void sendError(String localizationKey, Object... args) {
        commandSource.sendMessage(Texts.of(TextColors.RED, MessageFormat.format(localizationKey, args)));

    }
}
