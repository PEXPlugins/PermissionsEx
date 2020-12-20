plugins {
    id("pex-component")
    id("ca.stellardrift.localization")
}

useAutoService()
dependencies {
    api(project(":api"))
    implementation(project(":core"))
    implementation("com.google.guava:guava:21.0")

    implementation(platform("org.jdbi:jdbi3-bom:3.18.0"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-sqlobject")

    testImplementation(testFixtures(project(":core")))
}
