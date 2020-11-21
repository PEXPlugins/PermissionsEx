import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

open class PexPlatformExtension @Inject constructor(val relocationRoot: String, val task: TaskProvider<ShadowJar>) {
    fun relocate(vararg relocations: String) {
        this.task.configure {
            relocations.forEach {
                // TODO: Shorten this package somewhat?
                relocate(it, "$relocationRoot.$it")
            }
        }
    }
}