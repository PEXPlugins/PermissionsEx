import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate

fun Project.useImmutables() {
    val immutablesGroup = "org.immutables"
    val immutablesVersion: String by this

    dependencies {
        add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "$immutablesGroup:value:$immutablesVersion")
        add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, "$immutablesGroup:value:$immutablesVersion:annotations")
        add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "$immutablesGroup:builder:$immutablesVersion")
    }
}

fun Project.useCheckerFramework(): Dependency? {
    val checkerVersion: String by this

    return dependencies.add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, "org.checkerframework:checker-qual:$checkerVersion")
}

fun Project.useAutoService() {
    val autoServiceGroup = "com.google.auto.service"
    val autoServiceVersion: String by this

    dependencies {
        add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, "$autoServiceGroup:auto-service:$autoServiceVersion")
        add(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME, "$autoServiceGroup:auto-service-annotations:$autoServiceVersion")
    }
}