dependencies {
    implementation(project(":api"))
    annotationProcessor("com.google.auto.service:auto-service:1.0-rc7")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
}
