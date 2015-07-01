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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.util.Translations._;

public abstract class PermissionsExExecutor implements CommandExecutor {
    protected final PermissionsEx pex;
    static final Set<Map.Entry<String, String>> GLOBAL = ImmutableSet.of();

    protected PermissionsExExecutor(PermissionsEx pex) {
        this.pex = pex;
    }

    protected ImmutableOptionSubjectData getSubjectData(SubjectCache cache, String identifier) throws CommandException {
        try {
            return cache.getData(identifier, null);
        } catch (ExecutionException e) {
            throw new CommandException(_("Unable to get data for subject %s", identifier), e);
        }
    }

    protected <TextType> TextType formatContexts(Commander<TextType> src, Set<Map.Entry<String, String>> contexts) {
        return src.fmt().hl(contexts.isEmpty() ? src.fmt().tr(_("Global")) : src.fmt().combined(contexts.toString()));
    }

    protected Map.Entry<String, String> subjectOrSelf(Commander<?> src, CommandContext args) throws CommandException {
        if (args.hasAny("subject")) {
            return args.getOne("subject");
        } else {
            Optional<Map.Entry<String, String>> ret = src.getSubjectIdentifier();
            if (!ret.isPresent()) {
                throw new CommandException(_("A subject must be provided for this command!"));
            } else {
                return ret.get();
            }
        }
    }

    protected void checkSubjectPermission(final Commander<?> src, Map.Entry<String, String> subject, String basePermission) throws CommandException {
        if (!src.hasPermission(basePermission + '.' + subject.getKey() + '.' + subject.getValue())
                && (!subject.equals(src.getSubjectIdentifier().orNull()) || !src.hasPermission(basePermission + ".own"))) {
            throw new CommandException(_("You do not have permission to use this command!"));
        }
    }

    protected <TextType> void messageSubjectOnFuture(ListenableFuture<?> future, final Commander<TextType> src, final Translatable message) {
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {
                src.msg(message);
            }

            @Override
            public void onFailure(Throwable t) {
                src.msg(_("Error (%s) occurred while performing command task! Please see console for details: %s", t.getClass().getSimpleName(), t.getMessage()));
                pex.getLogger().error(_("Error occurred while executing command for user %s", src.getName()).translateFormatted(Locale.getDefault()), t);
            }
        });

    }
}
