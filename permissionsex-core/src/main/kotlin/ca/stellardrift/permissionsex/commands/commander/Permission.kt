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

package ca.stellardrift.permissionsex.commands.commander

import ca.stellardrift.permissionsex.util.SubjectIdentifier
import net.kyori.text.Component

/**
 * A representation of a permission.
 *
 * [value] is a dotted string
 * [description] describes the permission's purpose, including ways it may be specialized in use
 * [default] is the value to resolve this permission to when unset
 */
data class Permission(val value: String, val description: Component? = null, val default: Int = 0) {
    operator fun plus(other: SubjectIdentifier): Permission {
        val (name, subj) = other
        return Permission(value = "$value.$name.$subj", description = this.description, default = this.default)
    }

    operator fun plus(other: String): Permission {
        return Permission(value = "$value.$other", description = this.description, default = this.default)
    }
}

fun pexPerm(value: String) = Permission("permissionsex.$value")
