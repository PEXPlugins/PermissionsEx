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

package ca.stellardrift.permissionsex.proxycommon

import ca.stellardrift.permissionsex.context.ContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ninja.leaping.configurate.Types

/**
 * This context exists to mark the fact that a subject is operating in a proxy environment.
 */
object ProxyContextDefinition : ContextDefinition<Boolean>("proxy") {
    override fun serialize(userValue: Boolean): String = userValue.toString()
    override fun deserialize(canonicalValue: String): Boolean = Types.asBoolean(canonicalValue) ?: false
    override fun matches(ownVal: Boolean, testVal: Boolean): Boolean = ownVal == testVal
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: Boolean) -> Unit) =
        consumer(true)
}
