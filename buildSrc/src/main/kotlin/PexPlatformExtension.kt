import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject

abstract class PexPlatformExtension @Inject constructor(val relocationRoot: String, val task: TaskProvider<ShadowJar>, val project: Project) {

    @get:Inject
    abstract val toolchains: JavaToolchainService

    fun relocate(vararg relocations: String, keepElements: Int = 1) {
        this.task.configure {
            relocations.forEach {
                val shortenedPackageName = it.split('.')
                    .takeLast(keepElements)
                    .joinToString(".")
                relocate(it, "$relocationRoot.$shortenedPackageName")
            }
        }
    }

    fun excludeChecker() {
        this.task.configure {
            exclude("org/checkerframework/**")
        }
    }

    fun developmentRuntime(): Provider<JavaLauncher> {
        return project.extensions.getByType(JavaToolchainService::class).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(project.findProperty("developmentRuntime") as String))
        }
    }
}
