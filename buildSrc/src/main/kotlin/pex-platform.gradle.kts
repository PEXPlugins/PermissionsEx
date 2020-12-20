import ca.stellardrift.build.common.engineHub
import ca.stellardrift.build.common.pex
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("pex-component")
    id("ca.stellardrift.opinionated.kotlin")
    id("com.github.johnrengelman.shadow")
}

repositories {
    pex()
    engineHub()
}

tasks.processResources {
    expand("project" to project)
}

tasks.javadoc {
    enabled = false
}

val pexRelocationRoot: String by project
val shadowJar = tasks.named("shadowJar", ShadowJar::class) {
    inputs.property("pexRelocationRoot", pexRelocationRoot)

    // Caffeine reflectively accesses some of its cache implementations, so it can't be minimized
    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:.*:.*"))
    }

    // Don't shade compile-only annotations, or other project's module info files
    exclude("org/checkerframework/**")
    exclude("**/module-info.class")

    // Process service files
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(shadowJar)
}

extensions.create("pexPlatform", PexPlatformExtension::class, pexRelocationRoot, shadowJar)
