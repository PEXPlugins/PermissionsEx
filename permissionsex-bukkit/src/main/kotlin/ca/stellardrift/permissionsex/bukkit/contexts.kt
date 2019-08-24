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
package ca.stellardrift.permissionsex.bukkit

import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.context.EnumContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player

object WorldContextDefinition: ContextDefinition<String>("world") {
    override fun serialize(userValue: String): String {
        return userValue
    }

    override fun deserialize(canonicalValue: String): String {
        return canonicalValue // TODO: attempt to resolve world
    }

    override fun matches(ctx: ContextValue<String>, activeValue: String): Boolean {
        return ctx.getParsedValue(this).equals(activeValue, ignoreCase = true)
    }

    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
        val associated = subject.associatedObject.orElse(null)
        if (associated is Player) {
            consumer(associated.world.name)
        }
    }

    override fun suggestValues(subject: CalculatedSubject): Set<String> {
        return Bukkit.getWorlds().map(World::getName).toSet()
    }
}

object DimensionContextDefinition: EnumContextDefinition<World.Environment>("dimension", World.Environment::class.java) {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: World.Environment) -> Unit) {
        val associated = subject.associatedObject.orElse(null)
        if (associated != null && associated is Player) {
            consumer(associated.world.environment)
        }
    }
}