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
    `kotlin-dsl`
}

repositories {
    jcenter()
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net")
    }
    gradlePluginPortal()
}

dependencies {
    constraints {
        sequenceOf("asm", "asm-util", "asm-tree", "asm-analysis").forEach {
            implementation("org.ow2.asm:$it") {
                version { require("9.0") }
                because("Fabric's TinyRemapper requires ASM 9")
            }
        }
    }

    val opinionatedVersion = "4.0.1"
    val indraVersion = "1.1.1"
    implementation("ca.stellardrift:gradle-plugin-opinionated-common:$opinionatedVersion")
    implementation("ca.stellardrift:gradle-plugin-opinionated-kotlin:$opinionatedVersion")
    implementation("ca.stellardrift:gradle-plugin-opinionated-fabric:$opinionatedVersion")
    implementation("net.kyori:indra-common:$indraVersion")
    implementation("ca.stellardrift:gradle-plugin-localization:$opinionatedVersion")
    implementation("ca.stellardrift:gradle-plugin-templating:$opinionatedVersion")
    implementation("ca.stellardrift:gradle-plugin-configurate:$opinionatedVersion")
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")
    implementation("kr.entree:spigradle:2.2.3")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:1.3.0")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
