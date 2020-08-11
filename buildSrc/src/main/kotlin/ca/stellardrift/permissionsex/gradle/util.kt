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
 *
 */

package ca.stellardrift.permissionsex.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

// TODO: Replace this with bom subproject
enum class Versions(val version: String) {
    SPONGE("7.2.0"),
    CONFIGURATE("3.7.1"),
    SLF4J("1.7.30"),
    ANTLR("4.8-1"),
    KOTLINX_COROUTINES("1.3.8"),
    TEXT("3.0.4"),
    TEXT_ADAPTER("3.0.6");

    override fun toString(): String {
        return this.version
    }
}


fun Project.setupPublication() {
    extensions.getByType(PublishingExtension::class.java).publications.named("maven", MavenPublication::class.java).configure {
        it.from(components.getByName("java"))
    }
}
