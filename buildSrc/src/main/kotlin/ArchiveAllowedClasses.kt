import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import javax.inject.Inject

abstract class ArchiveAllowedClasses : DefaultTask() {

    /**
     * The `jar` file that will be checked
     */
    @get:InputFiles
    abstract val input: ConfigurableFileCollection

    @get:Input
    abstract val allowedPackages: SetProperty<String>

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
    }

    @TaskAction
    fun validate() {
        val tree = this.archiveOps.zipTree(input.singleFile)
        this.allowedPackages.finalizeValue()
        val packages = allowedPackages.get().map { it.replace('.', '/')}.toSet()

        val violations = mutableSetOf<String>()
        // Open a zip tree
        tree.matching {
            // Match: everything in a subdirectory
            include("**")
            include("META-INF/versions/**")
            // Except for non-versioned META-INF
            // And base files
            exclude("META-INF/**")
            exclude("*")
            packages.forEach {
                // And allowed packages
                exclude("$it/**")
                exclude("META-INF/versions/*/$it/**")
            }
        }.visit {
            // Then collect a list of violations
            if (!this.isDirectory) {
                violations.add(this.path)
            }
        }

        // And tell the user
        if (!violations.isEmpty()) {
            logger.error("Shading relocation violations detected: ")
            violations.chunked(2) { it.joinToString(", ")}.forEach {
                logger.error(it)
            }
            throw GradleException("Unrelocated classes detected, see above for details")
        }

    }

}
