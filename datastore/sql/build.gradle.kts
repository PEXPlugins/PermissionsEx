plugins {
    id("pex-component")
}

useAutoService()
dependencies {
    implementation(project(":api"))

    implementation(platform("org.jdbi:jdbi3-bom:3.18.0"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-sqlobject")
}
