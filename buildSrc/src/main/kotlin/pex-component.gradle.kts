import ca.stellardrift.build.common.stellardriftReleases
import ca.stellardrift.build.common.stellardriftSnapshots
import ca.stellardrift.build.localization.LocalizationExtension
import ca.stellardrift.build.localization.TemplateType
import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("ca.stellardrift.opinionated")
    id("net.kyori.indra.publishing")
    id("net.ltgt.errorprone")
}

repositories {
    stellardriftSnapshots()
    stellardriftReleases()
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
    // publishAllTo("pex", "https://repo.glaremasters.me/repository/permissionsex/")
    publishReleasesTo("stellardrift", "https://repo.stellardrift.ca/repository/releases/")
    publishSnapshotsTo("stellardrift", "https://repo.stellardrift.ca/repository/snapshots/")
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
        templateType.set(TemplateType.JAVA)
        templateFile.set(rootProject.file("etc/messages-template.java.tmpl"))
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure(KotlinJvmProjectExtension::class) {
            sourceSets.named("main") { kotlin.srcDirs(tasks.named("generateLocalization")) }
        }
    }
}

ktlint {
    filter {
        exclude("generated-src/**")
    }
}

// Errorprone
dependencies {
    val errorproneVersion: String by project
    compileOnly("com.google.errorprone:error_prone_annotations:$errorproneVersion")
    errorprone("com.google.errorprone:error_prone_core:$errorproneVersion")
}

tasks.withType(JavaCompile::class).configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
        excludedPaths.set(".*/(generated-src|mixin|accessor)/.*.java")
    }
}

/* extensions.getByType(LicenseExtension::class).apply {
    ext["year"] = LocalDate.now(ZoneOffset.UTC).year
} */
