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
package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.proxycommon.ProxyCommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import com.velocitypowered.api.permission.PermissionFunction
import com.velocitypowered.api.permission.PermissionSubject
import com.velocitypowered.api.permission.Tristate
import com.velocitypowered.api.proxy.Player

internal class PEXPermissionFunction(
    private val plugin: PermissionsExPlugin,
    private val source: PermissionSubject
) : PermissionFunction {
    val subject: CalculatedSubject by lazy {
        if (source is Player) {
            plugin.users[source.uniqueId]
        } else {
            plugin.manager.subject(IDENT_SERVER_CONSOLE)
        }.join()
    }

    override fun getPermissionValue(permission: String): Tristate {
        return subject.permission(permission).asTristate()
    }
}
