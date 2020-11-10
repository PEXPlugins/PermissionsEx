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

package ca.stellardrift.permissionsex.proxycommon;

import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.Map;

public final class ProxyCommon {
    private ProxyCommon() {}

    public static final String SUBJECTS_SYSTEM = "system";
    public static final Map.Entry<String, String> IDENT_SERVER_CONSOLE = UnmodifiableCollections.immutableMapEntry(SUBJECTS_SYSTEM, "Server");
}
