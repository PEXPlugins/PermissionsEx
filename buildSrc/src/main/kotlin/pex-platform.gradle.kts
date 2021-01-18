import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("pex-component")
    id("com.github.johnrengelman.shadow")
}

tasks {
    processResources {
        inputs.property("version", project.version)

        expand("project" to project)
    }

    javadoc {
        enabled = false
    }

    jar {
        manifest.attributes(
            "Specification-Title" to rootProject.name,
            "Specification-Version" to project.version,
            "Implementation-Title" to "${rootProject.name} ${project.name.capitalize()}",
            "Implementation-Version" to "${project.version}${project.findProperty("pexSuffix") ?: ""}"
        )
    }
}

val pexRelocationRoot: String by project
val shadowJar = tasks.named("shadowJar", ShadowJar::class) {
    inputs.property("pexRelocationRoot", pexRelocationRoot)

    // Caffeine reflectively accesses some of its cache implementations, so it can't be minimized
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
        exclude(project(":datastore:sql"))
    }

    // Don't shade compile-only annotations, or other project's module info files
    exclude("**/module-info.class")

    // Process service files
    mergeServiceFiles()
}

val validateShadowing by tasks.registering(ArchiveAllowedClasses::class) {
    input.from(shadowJar)
    allowedPackages.add("ca.stellardrift")
}

tasks.check {
    dependsOn(validateShadowing)
}

tasks.assemble {
    dependsOn(shadowJar)
}

extensions.create("pexPlatform", PexPlatformExtension::class, pexRelocationRoot, shadowJar, project)
