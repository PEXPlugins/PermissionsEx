
import ca.stellardrift.build.common.OpinionatedExtension
import ca.stellardrift.build.common.pex
import ca.stellardrift.build.common.sk89q
import ca.stellardrift.build.common.sonatypeOss
import net.minecrell.gradle.licenser.LicenseExtension
import java.time.LocalDate
import java.time.ZoneOffset

plugins {
    kotlin("jvm") version "1.4.0" apply false
    id("ca.stellardrift.opinionated.kotlin") version "3.1" apply false
    id("ca.stellardrift.opinionated.publish") version "3.1" apply false
    id("ca.stellardrift.localization") version "3.1" apply false
    id("ca.stellardrift.templating") version "3.1" apply false
    id("ca.stellardrift.configurate-transformations") version "3.1" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
    id("com.github.ben-manes.versions") version "0.36.0"
    `maven-publish`
}

group = "ca.stellardrift.permissionsex"
version = "2.0-SNAPSHOT"
description = project.property("pexDescription") as String

subprojects {
    if (name == "api") { // TODO: make this less hacky
        apply(plugin="ca.stellardrift.opinionated")
    } else {
        apply(plugin = "ca.stellardrift.opinionated.kotlin")
    }

    apply(plugin="ca.stellardrift.opinionated.publish")

    repositories {
        mavenCentral()
        sonatypeOss()
        jcenter()
        pex()
        sk89q()
    }

    extensions.getByType(OpinionatedExtension::class).apply {
        github("PEXPlugins", "PermissionsEx")
        apache2()
        publication?.apply {
            artifactId = "permissionsex-${project.name}"
            pom {
                developers {
                    developer {
                        name.set("zml")
                        email.set("zml [at] stellardrift [dot] ca")
                    }
                }
                ciManagement {
                    system.set("Jenkins")
                    url.set("https://jenkins.addstar.com.au/job/PermissionsEx")
                }
            }
        }
        publishTo("pex", "https://repo.glaremasters.me/repository/permissionsex/")
    }

    extensions.getByType(org.jlleitschuh.gradle.ktlint.KtlintExtension::class).apply {
        filter {
            exclude("generated-src/**")
        }
    }

    extensions.getByType(LicenseExtension::class).apply {
        header = rootProject.file("LICENSE_HEADER")
        ext["year"] = LocalDate.now(ZoneOffset.UTC).year
    }
}

tasks.withType(Jar::class).configureEach { // disable
    onlyIf { false }
}

val collectExcludes = ext["buildExcludes"].toString().split(',').toSet()

val collectImplementationArtifacts by tasks.registering(Copy::class) {
    val config = configurations.detachedConfiguration(
        *subprojects
            .filter { it.name !in collectExcludes && it.path.contains("platform:")}
            .map { dependencies.project(it.path, configuration = "shadow") }
            .toTypedArray()
    )
    config.isTransitive = false

    from(config)
    rename("(.+)-all(.+)", "PermissionsEx $1$2")
    into("$buildDir/libs")

    doFirst {
        if (destinationDir.exists()) {
            destinationDir.deleteRecursively()
        }
    }

}

tasks.register("build") {
    dependsOn(collectImplementationArtifacts)
    group = "build"
}


