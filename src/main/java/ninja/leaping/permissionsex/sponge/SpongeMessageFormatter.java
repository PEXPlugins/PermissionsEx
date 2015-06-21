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
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.ButtonType;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.util.command.CommandSource;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Factory to create formatted elements of messages
 */
public class SpongeMessageFormatter implements MessageFormatter<TextBuilder> {
    private static final Text EQUALS_SIGN = Texts.of(TextColors.GRAY, "=");
    private final PermissionsExPlugin pex;

    SpongeMessageFormatter(PermissionsExPlugin pex) {
        this.pex = pex;
    }

    @Override
    public TextBuilder subject(Map.Entry<String, String> subject) {
        Function<String, Optional<CommandSource>> func = pex.getCommandSourceProvider(subject.getKey());
        Optional<CommandSource> source = func == null ? Optional.<CommandSource>absent() : func.apply(subject.getValue());
        String name;
        if (source.isPresent()) {
            name = source.get().getName();
        } else {
            name = pex.getSubjects(subject.getKey()).get(subject.getValue()).getSubjectData().getOptions(SubjectData.GLOBAL_CONTEXT).get("name");
        }

        Text nameText;
        if (name != null) {
            nameText = Texts.of(Texts.of(TextColors.GRAY, subject.getValue()), "/", name);
        } else {
            nameText = Texts.of(subject.getValue());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        return Texts.builder().append(Texts.builder(subject.getKey()).style(TextStyles.BOLD).build(), Texts.of(" "),
                nameText).onHover(TextActions.showText(tr(_("Click to view more info")).build())).onClick(TextActions.runCommand("/pex " + subject.getKey() + " " + subject.getValue() + " info"));
    }

    @Override
    public TextBuilder ladder(RankLadder ladder) {
        return Texts.builder(ladder.getName())
                .style(TextStyles.BOLD)
                .onHover(TextActions.showText(tr(_("Click here to view more info")).build()))
                .onClick(TextActions.runCommand("/pex rank " + ladder.getName()));
    }

    @Override
    public TextBuilder booleanVal(boolean val) {
        return (val ? tr(_("true")) : tr(_("false"))).color(val ? TextColors.GREEN : TextColors.RED);
    }

    @Override
    public TextBuilder button(ButtonType type, Translatable label, @Nullable Translatable tooltip, String command, boolean execute) {
        TextBuilder builder = tr(label);
        TextColor textColor;
        switch (type) {
            case POSITIVE:
                textColor = TextColors.GREEN;
                break;
            case NEGATIVE:
                textColor = TextColors.RED;
                break;
            case NEUTRAL:
                textColor = TextColors.AQUA;
                break;
            default:
                throw new IllegalArgumentException("Provided with unknown ButtonType " + type);
        }
        builder.color(textColor);
        if (tooltip != null) {
            builder.onHover(TextActions.showText(tr(tooltip).build()));
        }
        if (execute) {
            builder.onClick(TextActions.runCommand(command));
        } else {
            builder.onClick(TextActions.suggestCommand(command));
        }
        return builder;
    }

    @Override
    public TextBuilder permission(String permission, int value) {
        TextColor valueColor;
        if (value > 0) {
            valueColor = TextColors.GREEN;
        } else if (value < 0) {
            valueColor = TextColors.RED;
        } else {
            valueColor = TextColors.GRAY;
        }
        return Texts.builder().append(Texts.of(valueColor, permission), EQUALS_SIGN, Texts.of(value));
    }

    @Override
    public TextBuilder option(String permission, String value) {
        return Texts.builder(permission).append(EQUALS_SIGN, Texts.of(value));
    }

    @Override
    public TextBuilder header(TextBuilder text) {
        return text.style(TextStyles.BOLD);
    }

    @Override
    public TextBuilder hl(TextBuilder text) {
        return text.color(TextColors.AQUA);
    }

    @Override
    public TextBuilder combined(Object... elements) {
        TextBuilder build = Texts.builder();
        for (Object el : elements) {
            if (el instanceof TextBuilder) {
                build.append(((TextBuilder) el).build());
            } else {
                build.append(Texts.of(el));
            }
        }
        return build;
    }

    @Override
    public TextBuilder tr(Translatable tr) {
        boolean unwrapArgs = false;
        for (Object arg: tr.getArgs()) {
            if (arg instanceof Translatable || arg instanceof TextBuilder) {
                unwrapArgs = true;
                break;
            }
        }
        Object[] args = tr.getArgs();
        if (unwrapArgs) {
            Object[] oldArgs = args;
            args = new Object[oldArgs.length];
            for (int i = 0; i < oldArgs.length; ++i) {
                Object arg = oldArgs[i];
                if (arg instanceof Translatable) {
                    arg = tr(tr).build();
                } else if (arg instanceof TextBuilder) {
                    arg = ((TextBuilder) arg).build();
                }
                args[i] = arg;
            }
        }
        return Texts.builder(new PEXTranslation(tr), args);
    }

    @NonnullByDefault
    static class PEXTranslation implements Translation {
        private final Translatable translation;

        PEXTranslation(Translatable translation) {
            this.translation = translation;
        }

        @Override
        public String getId() {
            return translation.getUntranslated();
        }

        @Override
        public String get(Locale locale) {
            return translation.translate(locale);
        }

        @Override
        public String get(Locale locale, Object... objects) {
            return translation.translateFormatted(locale);
        }
    }
}
