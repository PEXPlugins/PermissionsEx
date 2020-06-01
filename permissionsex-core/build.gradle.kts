
import ca.stellardrift.build.common.configurate
import ca.stellardrift.build.common.kyoriText
import ca.stellardrift.permissionsex.gradle.Versions

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

configurations.compile {
    exclude("org.antlr", "antlr4")
}

dependencies {
    antlr("org.antlr:antlr4:${Versions.ANTLR}")

    api(platform(configurate("bom", Versions.CONFIGURATE)))
    api(configurate("gson"))
    api(configurate("hocon"))
    implementation(configurate("yaml"))
    implementation(configurate("ext-kotlin", Versions.CONFIGURATE))
    implementation("com.github.ben-manes.caffeine:caffeine:2.7.0") {
        exclude("com.google.errorprone")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.KOTLINX_COROUTINES}")

    api("org.slf4j:slf4j-api:${Versions.SLF4J}")
    api(kyoriText("api", Versions.TEXT))
    api(kyoriText("feature-pagination", Versions.TEXT))
    implementation(kyoriText("serializer-plain", Versions.TEXT))
    implementation(kyoriText("serializer-legacy", Versions.TEXT))
    implementation("org.antlr:antlr4-runtime:${Versions.ANTLR}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLINX_COROUTINES}")

    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.4.3")
    testImplementation("org.postgresql:postgresql:42.2.6")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

opinionated {
    useJUnit5()
}

tasks.generateGrammarSource {
    val grammarPackage = "${project.group}.util.glob.parser"
    arguments.addAll(listOf("-package", grammarPackage))
    outputDirectory = File("$buildDir/generated-src/antlr/main/${grammarPackage.replace(".", "/")}")
}
