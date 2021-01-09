/*
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
package ca.stellardrift.permissionsex.minecraft.command.definition;

import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Elements;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import cloud.commandframework.Command;

import static ca.stellardrift.permissionsex.minecraft.command.Elements.*;

final class DeleteSubcommand {

    private DeleteSubcommand() {
    }

    static Command.Builder<Commander> register(
        final Command.Builder<Commander> build,
        final SubjectRefProvider subjectProvider
    ) {
        final Permission delete = Permission.pex("delete");
        return build
            .permission(delete)
            .handler(handler((source, engine, ctx) -> {
                final SubjectRef.ToData<?> data = subjectProvider.provideData(ctx, delete);
                data.isRegistered()
                    .thenCompose(registered -> {
                        if (!registered) {
                            throw new CommandException(Messages.DELETE_ERROR_DOES_NOT_EXIST.tr(source.formatter().subject(data)));
                        }
                        return data.remove();
                    })
                    .whenComplete(Elements.messageSender(source, Messages.DELETE_SUCCESS.tr(source.formatter().subject(data))));
            }));
    }

}
