
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.AbstractRunTask
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("pex-platform")
    id("ca.stellardrift.opinionated.fabric")
    id("ca.stellardrift.localization")
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

val minecraftVersion = "1.16.5"
dependencies {
    val adventurePlatformVersion: String by project
    val cloudVersion: String by project

    shade(project(":impl-blocks:minecraft")) {
        exclude("com.google.guava")
        exclude("com.google.code.gson")
        exclude("org.spongepowered")
        exclude("net.kyori")
        exclude("org.jetbrains.kotlin")
    }

    shade(project(":impl-blocks:hikari-config"))
    shade("org.apache.logging.log4j:log4j-slf4j-impl:2.8.1") { isTransitive = false }
    shade("cloud.commandframework:cloud-brigadier:$cloudVersion")

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$minecraftVersion+build.5:v2")
    modImplementation("net.fabricmc:fabric-loader:0.11.2")

    modImplementation("net.fabricmc.fabric-api:fabric-api:0.31.0+1.16")
    modImplementation(include("net.kyori:adventure-platform-fabric:$adventurePlatformVersion") {
        exclude("com.google.code.gson")
        exclude("ca.stellardrift", "colonel")
    })
    modImplementation(include("ca.stellardrift:confabricate:2.0.3") {
        exclude("com.google.code.gson")
    })
    modImplementation(include("me.lucko:fabric-permissions-api:0.1-SNAPSHOT")!!)

    runtimeOnly("net.minecraftforge:forgeflower:1.5.498.2")
}

loom {
    runs {
        configureEach {
            runDir(projectDir.resolve("run/").relativeTo(rootDir).toString())
        }
    }
}

tasks.withType(AbstractRunTask::class).configureEach {
    javaLauncher.set(pexPlatform.developmentRuntime())

    this.standardInput = System.`in`

    // Mixin debug options
    jvmArgs(
        // "-Dmixin.debug.verbose=true",
        // "-Dmixin.debug.export=true",
        // "-Dmixin.debug.export.decompile.async=false",
        "-Dmixin.dumpTargetOnFailure=true",
        "-Dmixin.checks.interfaces=true",
        "-Dpermissionsex.debug.mixinaudit=true"
    )

    // Configure mixin agent
    /*jvmArgumentProviders += CommandLineArgumentProvider {
        // Resolve the Mixin configuration
        // Java agent: the jar file for mixin
        val mixinJar = configurations.runtimeClasspath.get().resolvedConfiguration
            .getFiles { it.name == "sponge-mixin" && it.group == "net.fabricmc" }
            .atMostOne()
        if (mixinJar != null) {
            listOf("-javaagent:$mixinJar")
        } else {
            emptyList()
        }
    }*/
}

tasks.register("runFabricServer") {
    dependsOn(tasks.runServer)
    group = "pex"
    description = "Run a Fabric development server"
}

tasks.register("runFabricClient") {
    dependsOn(tasks.runClient)
    group = "pex"
    description = "Run a Fabric development client"
}

pexPlatform {
    relocate(
        "cloud.commandframework",
        "com.github.benmanes.caffeine",
        "com.zaxxer.hikari",
        "org.antlr",
        "org.jdbi",
        "org.pcollections",
        "org.slf4j"
    )
    relocate("org.apache.logging.slf4j", keepElements = 2)
    excludeChecker()
}

val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    archiveClassifier.set("dev-all")
    from(sourceSets["accessor"].output)
    from(sourceSets["mixin"].output)

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        exclude(dependency("org.jetbrains:annotations:.*"))
        exclude(dependency("io.leangen.geantyref:geantyref:.*"))
    }
}

val remapShadowJar = tasks.register<RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("all")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}

configurations {
    sequenceOf(shadowRuntimeElements, shadow).forEach {
        it.configure {
            outgoing {
                artifacts.clear()
                artifact(remapShadowJar)
            }
        }
    }
}
