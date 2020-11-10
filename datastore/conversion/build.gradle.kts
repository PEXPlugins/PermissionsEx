import ca.stellardrift.build.common.configurate

plugins {
    id("ca.stellardrift.opinionated.kotlin")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(configurate("yaml"))
    implementation(configurate("extra-kotlin"))
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
}
