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
package ninja.leaping.permissionsex.bukkit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.command.ButtonType;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.apache.commons.lang.LocaleUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.bukkit.BukkitTranslations.t;

/**
 * Factory to create formatted elements of messages
 */
public class BukkitMessageFormatter implements MessageFormatter<BaseComponent> {
    private static final BaseComponent EQUALS_SIGN = new TextComponent("=");
    static {
        EQUALS_SIGN.setColor(ChatColor.GRAY);
    }
    private final PermissionsExPlugin pex;
    private final CommandSender sender;

    BukkitMessageFormatter(PermissionsExPlugin pex, CommandSender sender) {
        this.pex = pex;
        this.sender = sender;
    }

    @Override
    public BaseComponent subject(SubjectRef subject) {
        String name;
        try {
            name = pex.getManager().getSubjects(subject.getType()).persistentData().getData(subject.getIdentifier(), null).get().getSegment(PermissionsEx.GLOBAL_CONTEXT).getOptions().get("name");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        BaseComponent nameText;
        if (name != null) {
            nameText = new TextComponent("");
            final BaseComponent child = new TextComponent(subject.getIdentifier());
            child.setColor(ChatColor.GRAY);
            nameText.addExtra(child);
            nameText.addExtra("/");
            nameText.addExtra(name);
        } else {
            nameText = new TextComponent(subject.getIdentifier());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        BaseComponent ret = new TextComponent("");
        BaseComponent typeComponent = new TextComponent(subject.getIdentifier());
        typeComponent.setBold(true);
        ret.addExtra(typeComponent);
        ret.addExtra(" ");
        ret.addExtra(nameText);
        ret.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{tr(t("Click to view more info"))}));
        ret.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex " + subject.getType() + " " + subject.getIdentifier() + " info"));
        return ret;
    }

    @Override
    public BaseComponent ladder(RankLadder ladder) {
        BaseComponent ret = new TextComponent(ladder.getName());
        ret.setBold(true);
        ret.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{tr(t("click here to view more info"))}));
        ret.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex rank " + ladder.getName()));
        return ret;
    }

    @Override
    public BaseComponent booleanVal(boolean val) {
        BaseComponent ret = (val ? tr(t("true")) : tr(t("false")));
        ret.setColor(val ? ChatColor.GREEN : ChatColor.RED);
        return ret;
    }

    @Override
    public BaseComponent button(ButtonType type, Translatable label, @Nullable Translatable tooltip, String command, boolean execute) {
        BaseComponent builder = tr(label);
        ChatColor textColor;
        switch (type) {
            case POSITIVE:
                textColor = ChatColor.GREEN;
                break;
            case NEGATIVE:
                textColor = ChatColor.RED;
                break;
            case NEUTRAL:
                textColor = ChatColor.AQUA;
                break;
            default:
                throw new IllegalArgumentException("Provided with unknown ButtonType " + type);
        }
        builder.setColor(textColor);
        if (tooltip != null) {
            builder.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{tr(tooltip)}));
        }
        builder.setClickEvent(new ClickEvent(execute ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND, command));
        return builder;
    }

    @Override
    public BaseComponent permission(String permission, Tristate value) {
        ChatColor valueColor;
        switch (value) {
            case TRUE:
                valueColor = ChatColor.GREEN;
                break;
            case FALSE:
                valueColor = ChatColor.RED;
                break;
            case UNDEFINED:
                valueColor = ChatColor.GRAY;
                break;
            default:
                throw new IllegalArgumentException("Invalid Tristate element passed in value: " + value);
        }
        BaseComponent ret = new TextComponent("");
        BaseComponent perm = new TextComponent(permission);
        perm.setColor(valueColor);
        ret.addExtra(perm);
        ret.addExtra(EQUALS_SIGN);
        ret.addExtra(String.valueOf(value));
        return ret;
    }

    @Override
    public BaseComponent option(String permission, String value) {
        BaseComponent ret = new TextComponent(permission);
        ret.addExtra(EQUALS_SIGN);
        ret.addExtra(value);
        return ret;
    }

    @Override
    public BaseComponent header(BaseComponent text) {
        text.setBold(true);
        return text;
    }

    @Override
    public BaseComponent hl(BaseComponent text) {
        text.setColor(ChatColor.AQUA);
        return text;
    }

    @Override
    public BaseComponent combined(Object... elements) {
        if (elements.length == 0) {
            return new TextComponent("");
        } else {
            BaseComponent base = componentFrom(elements[0]);
            for (int i = 1; i < elements.length; ++i) {
                base.addExtra(componentFrom(elements[i]));
            }
            return base;
        }
    }

    private BaseComponent componentFrom(Object obj) {
        if (obj instanceof Translatable) {
            return tr(((Translatable) obj));
        } else if (obj instanceof BaseComponent) {
            return ((BaseComponent) obj);
        } else {
            return new TextComponent(String.valueOf(obj));
        }
    }

    @Override
    public BaseComponent tr(Translatable tr) {
        Object[] oldArgs = tr.getArgs();
        Object[] args = new Object[oldArgs.length];
        for (int i = 0; i < oldArgs.length; ++i) {
            args[i] = componentFrom(oldArgs[i]);
        }
        return new TranslatableComponent(tr.translate(sender instanceof Player ? LocaleUtils.toLocale(((Player) sender).spigot().getLocale()) : Locale.getDefault()), args);
    }
}
