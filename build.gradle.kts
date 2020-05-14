
import ca.stellardrift.build.sk89q
import ca.stellardrift.build.sponge
import net.minecrell.gradle.licenser.LicenseExtension
import java.time.LocalDate
import java.time.ZoneOffset

plugins {
    id("ca.stellardrift.opinionated") version "2.0.1" apply false
    id("com.github.johnrengelman.shadow") version "5.2.0" apply false
    id("com.github.ben-manes.versions") version "0.25.0"
    `maven-publish`
}

group = "ca.stellardrift.permissionsex"
version = "2.0-SNAPSHOT"
description = project.property("pexDescription") as String

subprojects {
    apply(plugin="ca.stellardrift.opinionated.kotlin")
    apply(plugin="ca.stellardrift.opinionated.publish")

    repositories {
        mavenLocal()
        jcenter()
        /*maven(url  = "https://repo.glaremasters.me/repository/permissionsex") {
            name = "pex"
        }*/
        sponge()
        sk89q()
    }

    extensions.getByType(ca.stellardrift.build.OpinionatedExtension::class).apply {
        github("PEXPlugins", "PermissionsEx")
        apache2()
        publication?.apply {
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
    }

    extensions.getByType(LicenseExtension::class).apply {
        header = rootProject.file("LICENSE_HEADER")
        ext["year"] = LocalDate.now(ZoneOffset.UTC).year
    }

    publishing {
        repositories {
            if (project.hasProperty("pexUsername") && project.hasProperty("pexPassword")) {
                maven {
                    name = "pex"
                    url = java.net.URI("https://repo.glaremasters.me/repository/permissionsex")
                    credentials {
                        username = project.property("pexUsername").toString()
                        password = project.property("pexPassword").toString()
                    }
                }
            }
        }
    }
}

tasks.withType(Jar::class).configureEach { // disable
    onlyIf { false }
}

val collectExcludes = ext["buildExcludes"].toString().split(',').toSet()

val collectImplementationArtifacts by tasks.registering(Copy::class) {
    subprojects.forEach {
        if (it.name !in collectExcludes) {
            val outTask = it.tasks.findByPath("remapShadowJar") ?: it.tasks.findByPath("shadowJar")
            if (outTask != null) {
                from(outTask)
            }
        }
    }
    rename("(.+)-all(.+)", "$1$2")

    into("$buildDir/libs")
}

tasks.register("build") {
    dependsOn(collectImplementationArtifacts)
    group = "build"
}


