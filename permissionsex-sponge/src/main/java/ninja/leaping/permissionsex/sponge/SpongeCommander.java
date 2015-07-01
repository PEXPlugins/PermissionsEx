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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * An abstraction over the Sponge CommandSource that handles PEX-specific message formatting and localization
 */
public class SpongeCommander implements Commander<TextBuilder> {
    private final PermissionsExPlugin pex;
    private final CommandSource commandSource;
    private final SpongeMessageFormatter formatter;

    public SpongeCommander(PermissionsExPlugin pex, CommandSource commandSource) {
        this.pex = pex;
        this.commandSource = commandSource;
        this.formatter = new SpongeMessageFormatter(pex);
    }

    @Override
    public String getName() {
        return this.commandSource.getName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return commandSource.hasPermission(permission);
    }

    @Override
    public Locale getLocale() {
        return commandSource instanceof Player ? ((Player) commandSource).getLocale() : Locale.getDefault();
    }

    @Override
    public Optional<Map.Entry<String, String>> getSubjectIdentifier() {
        return Optional.of(Maps.immutableEntry(commandSource.getContainingCollection().getIdentifier(), commandSource.getIdentifier()));
    }

    @Override
    public Set<Map.Entry<String, String>> getActiveContexts() {
        return PEXOptionSubjectData.parSet(commandSource.getActiveContexts());
    }

    @Override
    public MessageFormatter<TextBuilder> fmt() {
        return formatter;
    }

    @Override
    public void msg(Translatable message) {
        msg(fmt().tr(message));
    }


    @Override
    public void debug(Translatable message) {
        debug(fmt().tr(message));
    }

    @Override
    public void error(Translatable message) {
        error(fmt().tr(message));
    }

    @Override
    public void msg(TextBuilder text) {
        commandSource.sendMessage(text.color(TextColors.DARK_AQUA).build());
    }

    @Override
    public void debug(TextBuilder text) {
        commandSource.sendMessage(text.color(TextColors.GRAY).build());
    }

    @Override
    public void error(TextBuilder text) {
        commandSource.sendMessage(text.color(TextColors.RED).build());
    }

    @Override
    public void msgPaginated(Translatable title, @Nullable Translatable header, final Iterable<TextBuilder> text) {
        PaginationBuilder build = pex.getGame().getServiceManager().provide(PaginationService.class).get().builder();

        build.title(fmt().hl(fmt().header(fmt().tr(title))).build());
        if (header != null) {
            build.header(fmt().tr(header).color(TextColors.GRAY).build());
        }
        build.contents(Iterables.transform(text, new Function<TextBuilder, Text>() {
            @Nullable
            @Override
            public Text apply(TextBuilder input) {
                return input.color(TextColors.DARK_AQUA).build();
            }
        }))
                .sendTo(commandSource);
    }
}
