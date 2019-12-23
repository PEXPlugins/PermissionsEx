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

import net.minecrell.gradle.licenser.LicenseExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import java.net.URI

enum class Versions(val version: String) {
    SHADOW("5.1.0"),
    SPONGE("7.1.0"),
    CONFIGURATE("3.6"),
    SLF4J("1.7.26"),
    ANTLR("4.7.2"),
    JUNIT("5.5.2");

    override fun toString(): String {
        return this.version
    }
}

fun DependencyHandler.configurate(comp: String): String {
    return "org.spongepowered:configurate-$comp:${Versions.CONFIGURATE}"
}

fun RepositoryHandler.spongeRepo(): ArtifactRepository {
    return maven {
        it.name = "sponge-repo"
        it.url = URI("https://repo.spongepowered.org/maven")
    }
}

fun RepositoryHandler.sk89qRepo(): ArtifactRepository = maven {
    it.name = "sk90q-repo"
    it.url = URI("https://maven.sk89q.com/repo/")
}

fun Project.useJUnit5(scope: String = "testImplementation") {
    dependencies.apply {
        add(scope, "org.junit.jupiter:junit-jupiter-engine:${Versions.JUNIT}")
        add(scope, "org.junit.jupiter:junit-jupiter-api:${Versions.JUNIT}")
        add(scope, "org.jetbrains.kotlin:kotlin-test")
        add(scope, "org.jetbrains.kotlin:kotlin-test-junit5")
    }
    tasks.withType(Test::class.java) {
        it.useJUnitPlatform()
    }
}

fun Project.applyCommonSettings() {
    repositories.apply {
        mavenCentral()
        spongeRepo()
        sk89qRepo()
    }


    afterEvaluate {
        // For reproducible builds, we want archives to be identical as much as possible
        tasks.withType(AbstractArchiveTask::class.java) {
            it.isPreserveFileTimestamps = false
            it.isReproducibleFileOrder = true
        }
    }

    // Java
    this.plugins.apply("java-library")
    extensions.configure(JavaPluginExtension::class.java) {
        it.sourceCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType(JavaCompile::class.java) {
        it.options.encoding = "UTF-8"
        it.options.compilerArgs.add("-Xlint:unchecked")
    }

    // Set up licensing
    this.plugins.apply("net.minecrell.licenser")
    extensions.configure(LicenseExtension::class.java) {
        it.header = rootProject.file("LICENSE_HEADER")

        it.exclude("**/*.yml") // Don't process yaml -- unnecessary, and causes test failures
        // Exclude unsupported file types to avoid warnings
        it.exclude("**/*.conf")
        it.exclude("**/*.sql")
        // Antlr
        it.exclude("**/*.g4")
        it.exclude("**/*.interp")
        it.exclude("**/*.tokens")
        it.exclude("**/glob/parser/*.java")
    }

    tasks.findByPath("assemble")?.apply {
        dependsOn(tasks.getByPath("updateLicenses"))
    }

    tasks.findByPath("check")?.apply {
        dependsOn(tasks.getByPath("checkLicenses"))
    }

}
