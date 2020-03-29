
import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.applyCommonSettings
import ca.stellardrift.permissionsex.gradle.configurate
import ca.stellardrift.permissionsex.gradle.setupJavadocSourcesJars
import ca.stellardrift.permissionsex.gradle.setupPublication
import ca.stellardrift.permissionsex.gradle.useJUnit5

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
    antlr
    id("ca.stellardrift.localization")
}


applyCommonSettings()
useJUnit5()
setupPublication()
setupJavadocSourcesJars()

configurations.compile {
    exclude("org.antlr", "antlr4")
}

dependencies {
    antlr("org.antlr:antlr4:${Versions.ANTLR}")

    api(configurate("gson"))
    api(configurate("hocon"))
    implementation(configurate("yaml"))
    implementation("com.github.ben-manes.caffeine:caffeine:2.7.0") {
        exclude("com.google.errorprone")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.KOTLINX_COROUTINES}")

    api("org.slf4j:slf4j-api:${Versions.SLF4J}")
    api("net.kyori:text-api:${Versions.TEXT}")
    api("net.kyori:text-feature-pagination:${Versions.TEXT}")
    implementation("net.kyori:text-serializer-plain:${Versions.TEXT}")
    implementation("net.kyori:text-serializer-legacy:${Versions.TEXT}")
    implementation("org.antlr:antlr4-runtime:${Versions.ANTLR}")


    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.4.3")
    testImplementation("org.postgresql:postgresql:42.2.6")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

/*val messagesDir = "src/main/messages"
val messagesTemplate = objects.fileProperty()
val messagesDestination = "$buildDir/generated-src/messages"
messagesTemplate.set(rootProject.file("etc/messages-template.kt.tmpl"))

val generateLocalization by tasks.creating {
    val templateEngine = groovy.text.StreamingTemplateEngine()
    inputs.file(messagesTemplate)
    inputs.dir(messagesDir)
    outputs.dir(messagesDestination)

    doLast {
        val tree = objects.fileTree().from(project.file(messagesDir))
        val template = templateEngine.createTemplate(messagesTemplate.get().asFile)

        tree.matching {
            include("**//*.properties")
            exclude { it.name.contains('_') }
        }.visit {
            if (this.isDirectory) {
                return@visit
            }
            val path = this.relativePath

            val packageName = path.parent.segments.joinToString(".").toLowerCase(Locale.ROOT)
            val className = path.lastName.split('.', limit = 2).first().capitalize()
            val destinationPath = path.replaceLastName("$className.kt").getFile(project.file(messagesDestination))

            val propertiesFile = Properties()
            FileReader(this.file).use {
                propertiesFile.load(it)
            }


            val templateData = mapOf(
                "packageName" to packageName,
                "className" to className,
                "keys" to propertiesFile.keys
            )

            destinationPath.parentFile.mkdirs()
            FileWriter(destinationPath).use {
                template.make(templateData).writeTo(it)
            }

        }
    }
}

extensions.getByType(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer::class).sourceSets["main"].apply {
    kotlin.srcDir(file(messagesDestination))
}
sourceSets["main"].resources.srcDir(messagesDir)
tasks.compileKotlin.get().dependsOn(generateLocalization)*/

tasks.generateGrammarSource {
    val grammarPackage = "${project.group}.util.glob.parser"
    arguments.addAll(listOf("-package", grammarPackage))
    outputDirectory = File("${buildDir}/generated-src/antlr/main/${grammarPackage.replace(".", "/")}")
}
