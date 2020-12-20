import ca.stellardrift.build.common.configurate

plugins {
    id("pex-component")
    id("ca.stellardrift.opinionated.kotlin")
    id("ca.stellardrift.localization")
}

tasks.javadoc {
    enabled = false
}

useAutoService()
dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(configurate("yaml"))
    implementation(configurate("extra-kotlin"))
}
