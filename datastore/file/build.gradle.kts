plugins {
    id("pex-component")
}

useAutoService()
dependencies {
    implementation(project(":api"))
}
