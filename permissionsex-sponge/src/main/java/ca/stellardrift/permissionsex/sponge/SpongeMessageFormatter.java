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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.command.ButtonType;
import ca.stellardrift.permissionsex.util.command.MessageFormatter;
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

import static ca.stellardrift.permissionsex.util.Util.castOptional;

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
    public Text.Builder subject(Map.Entry<String, String> subject) {
        Optional<CommandSource> source = castOptional(pex.getManager().getSubjects(subject.getKey()).getTypeInfo().getAssociatedObject(subject.getValue()), CommandSource.class);
        String name = source.map(CommandSource::getName)
                .orElseGet(() -> pex.getCollection(subject.getKey()).get().getSubject(subject.getValue()).map(subj -> subj.getSubjectData().getOptions(SubjectData.GLOBAL_CONTEXT).get("name")).orElse(null));

        Text nameText;
        if (name != null) {
            nameText = Text.of(Text.of(TextColors.GRAY, subject.getValue()), "/", name);
        } else {
            nameText = Text.of(subject.getValue());
        }

        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        return Text.builder().append(Text.builder(subject.getKey()).style(TextStyles.BOLD).build(), Text.of(" "),
                nameText).onHover(TextActions.showText(tr(SpongeTranslations.t("Click to view more info")).build())).onClick(TextActions.runCommand("/pex " + subject.getKey() + " " + subject.getValue() + " info"));
    }

    @Override
    public Text.Builder ladder(RankLadder ladder) {
        return Text.builder(ladder.getName())
                .style(TextStyles.BOLD)
                .onHover(TextActions.showText(tr(SpongeTranslations.t("Click here to view more info")).build()))
                .onClick(TextActions.runCommand("/pex rank " + ladder.getName()));
    }

    @Override
    public Text.Builder booleanVal(boolean val) {
        return (val ? tr(SpongeTranslations.t("true")) : tr(SpongeTranslations.t("false"))).color(val ? TextColors.GREEN : TextColors.RED);
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
    public Text.Builder permission(String permission, int value) {
        TextColor valueColor;
        if (value > 0) {
            valueColor = TextColors.GREEN;
        } else if (value < 0) {
            valueColor = TextColors.RED;
        } else {
            valueColor = TextColors.GRAY;
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
