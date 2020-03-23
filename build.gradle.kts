import ca.stellardrift.permissionsex.gradle.Versions
import ca.stellardrift.permissionsex.gradle.spongeRepo
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.ben-manes.versions") version "0.25.0"
    `maven-publish`
}
allprojects {
    group = "ca.stellardrift.permissionsex"
    version = "2.0-SNAPSHOT"
}

repositories {
    jcenter()
    spongeRepo()
}

subprojects {
    // TODO: Move this into buildSrc? Need to figure out how to get kotlin plugin
    //  into buildSrc classpath so we can work with it directly :(
    apply(plugin="kotlin")

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-XXLanguage:+InlineClasses", "-Xjvm-default=enable")
    }

    dependencies {
        "implementation"(kotlin("stdlib-jdk8"))
        "api"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KOTLINX_COROUTINES}")
    }

}

tasks.withType<Jar> { // disable
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


