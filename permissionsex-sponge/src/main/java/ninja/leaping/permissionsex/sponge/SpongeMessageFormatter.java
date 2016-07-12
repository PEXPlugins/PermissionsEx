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

import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.command.ButtonType;
import ninja.leaping.permissionsex.util.command.MessageFormatter;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static ninja.leaping.permissionsex.sponge.SpongeTranslations.t;

/**
 * Factory to create formatted elements of messages
 */
class SpongeMessageFormatter implements MessageFormatter<Text.Builder> {
    private static final Text EQUALS_SIGN = Text.of(TextColors.GRAY, "=");
    private final PermissionsExPlugin pex;

    SpongeMessageFormatter(PermissionsExPlugin pex) {
        this.pex = pex;
    }

    @Override
    public Text.Builder subject(SubjectRef subject) {
        Optional<CommandSource> source = pex.getCommandSourceProvider(subject.getType()).apply(subject.getIdentifier());
        String name;
        if (source.isPresent()) {
            name = source.get().getName();
        } else {
            name = pex.getSubjects(subject.getType()).get(subject.getIdentifier()).getSubjectData().getOptions(SubjectData.GLOBAL_CONTEXT).get("name");
        }

        Text nameText;
        if (name != null) {
            nameText = Text.of(Text.of(TextColors.GRAY, subject.getIdentifier()), "/", name);
        } else {
            nameText = Text.of(subject.getIdentifier());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        return Text.builder().append(Text.builder(subject.getType()).style(TextStyles.BOLD).build(), Text.of(" "),
                nameText).onHover(TextActions.showText(tr(t("Click to view more info")).build())).onClick(TextActions.runCommand("/pex " + subject.getType() + " " + subject.getIdentifier() + " info"));
    }

    @Override
    public Text.Builder ladder(RankLadder ladder) {
        return Text.builder(ladder.getName())
                .style(TextStyles.BOLD)
                .onHover(TextActions.showText(tr(t("Click here to view more info")).build()))
                .onClick(TextActions.runCommand("/pex rank " + ladder.getName()));
    }

    @Override
    public Text.Builder booleanVal(boolean val) {
        return (val ? tr(t("true")) : tr(t("false"))).color(val ? TextColors.GREEN : TextColors.RED);
    }

    @Override
    public Text.Builder button(ButtonType type, Translatable label, @Nullable Translatable tooltip, String command, boolean execute) {
        Text.Builder builder = tr(label);
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
    public Text.Builder permission(String permission, Tristate value) {
        TextColor valueColor;
        switch (value) {
            case TRUE:
                valueColor = TextColors.GREEN;
                break;
            case FALSE:
                valueColor = TextColors.RED;
                break;
            case UNDEFINED:
                valueColor = TextColors.GRAY;
                break;
            default:
                throw new IllegalArgumentException("Unknown Tristate value " + value);
        }
        return Text.builder().append(Text.of(valueColor, permission), EQUALS_SIGN, Text.of(value));
    }

    @Override
    public Text.Builder option(String permission, String value) {
        return Text.builder(permission).append(EQUALS_SIGN, Text.of(value));
    }

    @Override
    public Text.Builder header(Text.Builder text) {
        return text.style(TextStyles.BOLD);
    }

    @Override
    public Text.Builder hl(Text.Builder text) {
        return text.color(TextColors.AQUA);
    }

    @Override
    public Text.Builder combined(Object... elements) {
        Text.Builder build = Text.builder();
        for (Object el : elements) {
            if (el instanceof Text.Builder) {
                build.append(((Text.Builder) el).build());
            } else {
                build.append(Text.of(el));
            }
        }
        return build;
    }

    @Override
    public Text.Builder tr(Translatable tr) {
        boolean unwrapArgs = false;
        for (Object arg: tr.getArgs()) {
            if (arg instanceof Translatable || arg instanceof Text.Builder) {
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
                } else if (arg instanceof Text.Builder) {
                    arg = ((Text.Builder) arg).build();
                }
                args[i] = arg;
            }
        }
        return Text.builder(new PEXTranslation(tr), args);
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
