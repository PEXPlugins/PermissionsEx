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

package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.SubjectIdentifier

private const val PERMISSIONSEX_BASE = "permissionsex"

inline class Permission(val perm: String) {
    companion object {
        fun forPex(perm: String): Permission {
            return Permission("$PERMISSIONSEX_BASE.$perm")
        }
    }

    operator fun plus(other: String): Permission {
        return Permission("$perm.$other")
    }

    operator fun plus(other: CalculatedSubject): Permission {
        return this + other.identifier
    }

    operator fun plus(other: SubjectIdentifier): Permission {
        return Permission("$perm.${other.key}.${other.value}")
    }

    fun has(subj: CalculatedSubject): Boolean {
        return subj.hasPermission(perm)
    }

    fun has(cmd: Commander): Boolean {
        return cmd.hasPermission(perm)
    }
}
