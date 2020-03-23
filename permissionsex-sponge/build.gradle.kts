
import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.setupPublication
import ca.stellardrift.permissionsex.gradle.useJUnit5
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

plugins {
    id("com.github.johnrengelman.shadow")
    kotlin("kapt")
    id("ca.stellardrift.localization")
}

applyCommonSettings()
useJUnit5()
setupPublication()

dependencies {
    api(project(":permissionsex-core")) {
        exclude("org.spongepowered")
        exclude("com.google.guava", "guava")
        exclude("org.slf4j", "slf4j-api")
        exclude("com.github.ben-manes.caffeine", "caffeine")
    }

    implementation("net.kyori:text-adapter-spongeapi:${Versions.TEXT}")

    kapt("org.spongepowered:spongeapi:${Versions.SPONGE}")
    testImplementation("org.spongepowered:spongeapi:${Versions.SPONGE}")
    shadow("org.spongepowered:spongeapi:${Versions.SPONGE}")

    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

// Expand tokens in source templates
val generatedSourceRoot = "$buildDir/generated/java-templates"
val generateSource by tasks.registering(Copy::class) {
    from("src/main/java-templates") {
        include("**/*.java")
    }
    expand("project" to project)
    into(generatedSourceRoot)
}

sourceSets["main"].java.srcDir(generatedSourceRoot)
tasks.compileKotlin {
    dependsOn(generateSource)
}

val relocateRoot = project.ext["pexRelocateRoot"]
val shadowJar by tasks.getting(ShadowJar::class) {
    minimize()
    listOf("org.antlr",
        "net.kyori.text",
        "org.jetbrains.annotations").forEach {
        relocate(it, "$relocateRoot.$it")
    }

    manifest {
        attributes("Automatic-Module-Name" to project.name)
    }
}

tasks.assemble {
    dependsOn(shadowJar)
}
