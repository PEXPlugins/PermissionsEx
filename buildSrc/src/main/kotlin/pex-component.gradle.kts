import ca.stellardrift.build.common.engineHub
import ca.stellardrift.build.common.pex
import ca.stellardrift.build.localization.LocalizationExtension
import net.kyori.indra.sonatypeSnapshots

plugins {
    id("ca.stellardrift.opinionated")
    id("net.kyori.indra.publishing")
}

repositories {
    mavenCentral()
    sonatypeSnapshots()
    jcenter()
}

indra {
    github("PEXPlugins", "PermissionsEx")
    apache2License()

    javaVersions {
        testWith(8, 11, 15)
    }

    configurePublications {
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
    publishAllTo("pex", "https://repo.glaremasters.me/repository/permissionsex/")
}

opinionated {
    automaticModuleNames = true
}

// Testing dependency
val junitVersion: String by project
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

// If we have localization plugin, configure it
plugins.withId("ca.stellardrift.localization") {
    extensions.configure(LocalizationExtension::class.java) {
        templateFile.set(rootProject.file("etc/messages-template.kt.tmpl"))
    }
}

ktlint {
    filter {
        exclude("generated-src/**")
    }
}

/* extensions.getByType(LicenseExtension::class).apply {
    ext["year"] = LocalDate.now(ZoneOffset.UTC).year
} */
