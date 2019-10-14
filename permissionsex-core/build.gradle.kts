
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
    api("org.slf4j:slf4j-api:${Versions.SLF4J}")
    implementation("org.antlr:antlr4-runtime:${Versions.ANTLR}")

    // Reactive libraries
    api("org.reactivestreams:reactive-streams:1.0.3")
    api(platform("io.projectreactor:reactor-bom:Dysprosium-RELEASE"))
    api("io.projectreactor:reactor-core")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("io.r2dbc:r2dbc-client:0.8.0.RC1")


    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("com.h2database:h2:1.4.199")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.4.3")
    testImplementation("org.postgresql:postgresql:42.2.6")
}

tasks.generateGrammarSource {
    val grammarPackage = "${project.group}.util.glob.parser"
    arguments.addAll(listOf("-package", grammarPackage))
    outputDirectory = File("${buildDir}/generated-src/antlr/main/${grammarPackage.replace(".", "/")}")
}