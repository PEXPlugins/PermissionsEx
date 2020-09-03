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

package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.util.optionally
import java.util.HashMap
import java.util.Optional
import java.util.concurrent.CompletableFuture
import net.kyori.adventure.text.Component
import org.spongepowered.api.service.permission.PermissionDescription
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.plugin.PluginContainer

/**
 * Implementation of a permission description
 */
class PEXPermissionDescription private constructor(
    private val service: PermissionsExService,
    private val permId: String,
    description: Component?,
    owner: PluginContainer?
) : PermissionDescription {
    private val description: Optional<Component> = description.optionally()
    private val owner: Optional<PluginContainer> = owner.optionally()

    override fun getId(): String {
        return this.permId
    }

    override fun getDescription(): Optional<Component> {
        return this.description
    }

    override fun findAssignedSubjects(type: String): CompletableFuture<Map<SubjectReference, Boolean>> {
        return this.service.loadCollection(type).thenCompose { it.getAllWithPermission(id) }
    }

    override fun getAssignedSubjects(collectionIdentifier: String): Map<Subject, Boolean> {
        return this.service.getCollection(collectionIdentifier).map { it.getLoadedWithPermission(id) }.orElseGet { emptyMap() }
    }

    override fun getOwner(): Optional<PluginContainer> {
        return this.owner
    }

    internal class Builder(private val owner: PluginContainer, private val service: PermissionsExService) : PermissionDescription.Builder {
        private var id: String? = null
        private var description: Component? = null
        private val ranks: MutableMap<String, Int> = HashMap()

        override fun id(id: String): Builder {
            this.id = id
            return this
        }

        override fun description(text: Component?): Builder {
            this.description = text
            return this
        }

        override fun assign(s: String, b: Boolean): Builder {
            return assign(s, if (b) 1 else -1)
        }

        fun assign(rankTemplate: String, power: Int): Builder {
            this.ranks[rankTemplate] = power
            return this
        }

        @Throws(IllegalStateException::class)
        override fun register(): PEXPermissionDescription {
            val ret = PEXPermissionDescription(this.service, this.id!!, this.description, this.owner)
            this.service.registerDescription(ret, this.ranks)
            return ret
        }
    }
}
