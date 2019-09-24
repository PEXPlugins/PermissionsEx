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
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.Util;
import ca.stellardrift.permissionsex.util.command.ButtonType;
import ca.stellardrift.permissionsex.util.command.MessageFormatter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    /**
     * Take a locale string provided from a minecraft client and attempt to parse it as a locale.
     * These are not strictly compliant with the iso standard, so we try to make things a bit more normalized.
     *
     * @param mcLocaleString The locale string, in the format provided by the Minecraft client
     * @return A Locale object matching the provided locale string
     */
    public static Locale toLocale(String mcLocaleString) {
        String[] parts = mcLocaleString.split("_", 3);
        switch (parts.length) {
            case 0:
                return Locale.getDefault();
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                throw new IllegalArgumentException("Provided locale '" + mcLocaleString + "' was not in a valid format!");
        }
    }

    @Override
    public BaseComponent subject(Map.Entry<String, String> subject) {
        SubjectType subjType = pex.getManager().getSubjects(subject.getKey());
        String name = Util.castOptional(subjType.getTypeInfo().getAssociatedObject(subject.getValue()), CommandSender.class).map(CommandSender::getName).orElse(null);
        if (name == null) {
            try {
                name = subjType.persistentData().getData(subject.getValue(), null).get().getOptions(PermissionsEx.GLOBAL_CONTEXT).get("name");
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        BaseComponent nameText;
        if (name != null) {
            nameText = new TextComponent("");
            final BaseComponent child = new TextComponent(subject.getValue());
            child.setColor(ChatColor.GRAY);
            nameText.addExtra(child);
            nameText.addExtra("/");
            nameText.addExtra(name);
        } else {
            nameText = new TextComponent(subject.getValue());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        BaseComponent ret = new TextComponent("");
        BaseComponent typeComponent = new TextComponent(subject.getKey());
        typeComponent.setBold(true);
        ret.addExtra(typeComponent);
        ret.addExtra(" ");
        ret.addExtra(nameText);
        ret.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{tr(BukkitTranslations.t("Click to view more info"))}));
        ret.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex " + subject.getKey() + " " + subject.getValue() + " info"));
        return ret;
    }

    @Override
    public BaseComponent ladder(RankLadder ladder) {
        BaseComponent ret = new TextComponent(ladder.getName());
        ret.setBold(true);
        ret.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{tr(BukkitTranslations.t("click here to view more info"))}));
        ret.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex rank " + ladder.getName()));
        return ret;
    }

    @Override
    public BaseComponent booleanVal(boolean val) {
        BaseComponent ret = (val ? tr(BukkitTranslations.t("true")) : tr(BukkitTranslations.t("false")));
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
    public BaseComponent permission(String permission, int value) {
        ChatColor valueColor;
        if (value > 0) {
            valueColor = ChatColor.GREEN;
        } else if (value < 0) {
            valueColor = ChatColor.RED;
        } else {
            valueColor = ChatColor.GRAY;
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
        return new TranslatableComponent(tr.translate(sender instanceof Player ? toLocale(((Player) sender).getLocale()) : Locale.getDefault()), args);
    }
}
