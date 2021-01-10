import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

open class PexPlatformExtension @Inject constructor(val relocationRoot: String, val task: TaskProvider<ShadowJar>) {
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
}
