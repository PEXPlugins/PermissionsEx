plugins {
    id("com.github.ben-manes.versions") version "0.50.0"
}

val pexDescription: String by project
group = "ca.stellardrift.permissionsex"
version = "2.0-SNAPSHOT"
description = pexDescription

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


