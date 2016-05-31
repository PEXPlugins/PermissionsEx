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
package ninja.leaping.permissionsex.command;

import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.Change;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static ninja.leaping.permissionsex.util.Translations.t;

public abstract class PermissionsExExecutor implements CommandExecutor {
    protected final PermissionsEx pex;

    protected PermissionsExExecutor(PermissionsEx pex) {
        this.pex = pex;
    }

    protected <TextType> TextType formatContexts(Commander<TextType> src, Set<Map.Entry<String, String>> contexts) {
        return src.fmt().hl(contexts.isEmpty() ? src.fmt().tr(t("Global")) : src.fmt().combined(contexts.toString()));
    }

    protected CalculatedSubject subjectOrSelf(Commander<?> src, CommandContext args) throws CommandException {
        try {
            if (args.hasAny("subject")) {
                SubjectRef ret = args.getOne("subject");
                return pex.getSubjects(ret.getType()).get(ret.getIdentifier()).get();
            } else {
                Optional<SubjectRef> ret = src.getSubjectIdentifier();
                if (!ret.isPresent()) {
                    throw new CommandException(t("A subject must be provided for this command!"));
                } else {
                    return pex.getSubjects(ret.get().getType()).get(ret.get().getIdentifier()).get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CommandException(t("Unable to get subject"), e);
        }
    }

    protected <TextType> SubjectDataReference getDataRef(Commander<TextType> src, CommandContext args, String permission) throws CommandException {
        CalculatedSubject subject = subjectOrSelf(src, args);
        checkSubjectPermission(src, subject.getIdentifier(), permission);
        return args.hasAny("transient") ? subject.transientData() : subject.data();
    }

    protected <TextType> CompletableFuture<Change<ImmutableSubjectData>> updateDataSegment(Commander<TextType> src, CommandContext args, String permission, Function<DataSegment, DataSegment> updateFunc, BiFunction<CalculatedSubject, Set<Map.Entry<String, String>>, Translatable> messageProvider) throws CommandException {
        CompletableFuture<CalculatedSubject> subj;
        if (args.hasAny("subject")) {
            SubjectRef ret = args.getOne("subject");
            subj = pex.getSubjects(ret.getType()).get(ret.getIdentifier());
        } else {
            Optional<SubjectRef> ret = src.getSubjectIdentifier();
            if (!ret.isPresent()) {
                throw new CommandException(t("A subject must be provided for this command!"));
            } else {
                subj = pex.getSubjects(ret.get().getType()).get(ret.get().getIdentifier());
            }
        }
        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.getAll("context"));
        boolean inheritable = !args.hasAny("noninheritable");
        int weight = args.hasAny("weight") ? args.getOne("weight") : 0;
        AtomicReference<Translatable> msg = new AtomicReference<>();

        CompletableFuture<Change<ImmutableSubjectData>> ret = subj.thenCompose(subject -> {
            runtimeCheckSubjectPermission(src, subject.getIdentifier(), permission);
            SubjectDataReference dat = args.hasAny("transient") ? subject.transientData() : subject.data();
            msg.set(messageProvider.apply(subject, contexts));
            return dat.update(old -> old.updateOrCreateSegment(contexts, weight, inheritable, updateFunc));
        });
        messageSubjectOnFuture(ret, src, msg::get);
        return ret;
    }

    protected void runtimeCheckSubjectPermission(final Commander<?> src, SubjectRef subject, String basePermission) throws RuntimeCommandException {
        if (!src.hasPermission(basePermission + '.' + subject.getType() + '.' + subject.getIdentifier())
                && (!subject.equals(src.getSubjectIdentifier().orElse(null)) || !src.hasPermission(basePermission + ".own"))) {
            throw new RuntimeCommandException(t("You do not have permission to use this command!"));
        }
    }


    protected void checkSubjectPermission(final Commander<?> src, SubjectRef subject, String basePermission) throws CommandException {
        if (!src.hasPermission(basePermission + '.' + subject.getType() + '.' + subject.getIdentifier())
                && (!subject.equals(src.getSubjectIdentifier().orElse(null)) || !src.hasPermission(basePermission + ".own"))) {
            throw new CommandException(t("You do not have permission to use this command!"));
        }
    }

    protected <TextType> void messageSubjectOnFuture(CompletableFuture<?> future, final Commander<TextType> src, final Translatable message) {
        messageSubjectOnFuture(future, src, () -> message);
    }

    protected <TextType> void messageSubjectOnFuture(CompletableFuture<?> future, final Commander<TextType> src, final Supplier<Translatable> message) {
        future.thenRun(() -> src.msg(message.get())).exceptionally(err -> {
            if (err instanceof CompletionException && err.getCause() != null) {
                err = err.getCause();
            }

            if (err instanceof RuntimeCommandException) {
                src.error(((RuntimeCommandException) err).getTranslatedMessage());
            } else {
                src.error(t("Error (%s) occurred while performing command task! Please see console for details: %s", err.getClass().getSimpleName(), err.getMessage()));
                pex.getLogger().error(t("Error occurred while executing command for user %s", src.getName()), err);
            }
            return null;
        });
    }

    static class RuntimeCommandException extends RuntimeException {
        private final Translatable message;

        RuntimeCommandException(Translatable message) {
            super(message.getUntranslated());
            this.message = message;
        }

        public Translatable getTranslatedMessage() {
            return this.message;
        }
    }
}
