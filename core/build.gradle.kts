
import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate
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
    implementation(configurate("extra-kotlin"))
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.6") {
        exclude("com.google.errorprone")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Versions.KOTLINX_COROUTINES}")
    implementation("com.google.guava:guava:21.0")

    api("org.slf4j:slf4j-api:${Versions.SLF4J}")
    api(adventure("api", Versions.TEXT))
    implementation(adventure("text-serializer-plain", Versions.TEXT))
    implementation(adventure("text-serializer-legacy", Versions.TEXT))
    implementation("org.antlr:antlr4-runtime:${Versions.ANTLR}")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLINX_COROUTINES}")

    testImplementation("org.slf4j:slf4j-jdk14:${Versions.SLF4J}")
    testImplementation("org.mockito:mockito-core:3.6.0")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.7.0")
    testImplementation("org.postgresql:postgresql:42.2.18")
}

localization {
    templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
}

opinionated {
    useJUnit5()
}

tasks.generateGrammarSource {
    this.arguments.addAll(listOf("-visitor", "-no-listener"))
}
