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
package ca.stellardrift.permissionsex.sponge.command;

import cloud.commandframework.meta.CommandMeta;
import org.spongepowered.api.text.Text;

public final class SpongeApi7MetaKeys {

    private SpongeApi7MetaKeys() {
    }

    public static final CommandMeta.Key<Text> RICH_DESCRIPTION = CommandMeta.Key.of(Text.class, "pex:sponge_rich_description");
    public static final CommandMeta.Key<Text> RICH_LONG_DESCRIPTION = CommandMeta.Key.of(Text.class, "pex:sponge_rich_long_description");

}
