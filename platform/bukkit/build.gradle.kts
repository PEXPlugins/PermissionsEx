import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate
import ca.stellardrift.build.common.gpl3
import ca.stellardrift.build.common.spigot
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.LocalDate
import java.time.ZoneOffset
import kr.entree.spigradle.data.Load
import kr.entree.spigradle.kotlin.spigot

plugins {
    id("ca.stellardrift.opinionated.kotlin")
    id("com.github.johnrengelman.shadow")
    id("pex-platform")
    id("ca.stellardrift.localization")
    id("kr.entree.spigradle")
}

repositories {
    spigot()
    maven(url = "https://jitpack.io/") {
        name = "jitpack"
        content {
            includeGroup("com.github.MilkBowl")
        }
    }
}

indra {
    gpl3()
}

java {
    registerFeature("h2dbSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

license {
    header = file("LICENSE_HEADER")
    ext["year"] = LocalDate.now(ZoneOffset.UTC).year
}

dependencies {
    val spigotVersion: String = "1.15.1-R0.1-SNAPSHOT"
    val adventurePlatformVersion: String by project
    val slf4jVersion: String by project

    api(project(":impl-blocks:minecraft")) {
        exclude(group = "com.google.guava")
        exclude("org.yaml", "snakeyaml")
        exclude("com.google.code.gson", "gson")
    }

    implementation(configurate("yaml")) {
        exclude("org.yaml", "snakeyaml")
    }
    implementation(adventure("platform-bukkit", adventurePlatformVersion)) {
        exclude("com.google.code.gson")
    }

    implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
    implementation(project(":impl-blocks:hikari-config"))

    // provided at runtime
    shadow(spigot(spigotVersion))
    shadow("com.github.MilkBowl:VaultAPI:1.7")
    shadow("com.sk89q.worldguard:worldguard-bukkit:7.0.4")
    shadow("com.h2database:h2:1.4.200")
}

spigot {
    val pexDescription: String by project
    val pexSuffix: String by project
    name = rootProject.name
    version = "${project.version}$pexSuffix"
    description = pexDescription
    apiVersion = "1.13"
    load = Load.STARTUP
    softDepends("Vault", "WorldGuard")

    commands {
        create("permissionsex") {
            aliases("pex")
            description = "Command for PermissionsEx"
        }

        create("promote") {
            aliases("prom", "rankup")
            description = "Promote a user"
        }

        create("demote") {
            aliases("dem", "rankdown")
            description = "Demote a user"
        }
    }
}

pexPlatform {
    relocate(
        "com.github.benmanes.caffeine",
        "com.typesafe",
        "com.zaxxer.hikari",
        "com.google.errorprone",
        "io.leangen.geantyref",
        "kotlinx",
        "kotlin",
        "net.kyori",
        "org.antlr",
        "org.jetbrains.annotations",
        "org.slf4j",
        "org.spongepowered.configurate"
    )
}

val shadowJar by tasks.getting(ShadowJar::class) {
    dependencies {
        exclude("org.yaml:snakeyaml")
    }
    exclude("org/checkerframework/**")
}
