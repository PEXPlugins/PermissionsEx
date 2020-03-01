/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ca.stellardrift.permissionsex.gradle

import groovy.text.StreamingTemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import java.io.FileReader
import java.io.FileWriter
import java.util.Locale
import java.util.Properties

val MESSAGES_ROOT_NAME = "messages"

enum class TemplateType {
    JAVA, KOTLIN, OTHER
}

open class LocalizationExtension(objects: ObjectFactory) {
    var templateFile: RegularFileProperty = objects.fileProperty()
    val templateType: TemplateType = TemplateType.KOTLIN
}

open class LocalizationGenerate : DefaultTask() {
    private val templateEngine = StreamingTemplateEngine()

    @InputDirectory
    @SkipWhenEmpty
    val resourceBundleSources = project.objects.directoryProperty()

    @Internal
    val tree = project.objects.fileTree().from(resourceBundleSources)

    @InputFile
    val templateFile = project.objects.fileProperty()

    @OutputDirectory
    val generatedSourcesOut = project.objects.directoryProperty()

    init {
        tree.include("**/*.properties")
        tree.exclude { it2 -> it2.name.contains('_') }
    }

    @TaskAction
    fun generateSources() {
        val template = templateEngine.createTemplate(templateFile.get().asFile)
        tree.visit {
            if (it.isDirectory) {
                return@visit
            }
            val path = it.relativePath

            val packageName = path.parent.segments.joinToString(".").toLowerCase(Locale.ROOT)
            val className = path.lastName.split('.', limit = 2).first().capitalize()
            val destinationPath = path.replaceLastName("$className.kt").getFile(generatedSourcesOut.asFile.get())

            val propertiesFile = Properties()
            FileReader(it.file).use {read ->
                propertiesFile.load(read)
            }

            val templateData = mapOf(
                "bundleName" to "$packageName.${path.lastName.split('.', limit = 2).first()}",
                "packageName" to packageName,
                "className" to className,
                "keys" to propertiesFile.keys
            )

            destinationPath.parentFile.mkdirs()
            FileWriter(destinationPath).use { write ->
                template.make(templateData).writeTo(write)
            }

        }
    }
}

/**
 * The localization plugin takes a source tree of `src/main/messages` containing resource bundle properties files,
 * and generates one class per resource bundle with a field for each key in the bundle.
 *
 * This becomes another resource root. In addition
 */
class LocalizationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("localization", LocalizationExtension::class.java)
        val parentTask = project.tasks.register("generateAllLocalizations")

        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.forEach {
            val messagesFileBasedir = project.file("src/${it.name}/$MESSAGES_ROOT_NAME")
            val outDir = project.layout.buildDirectory.dir("generated-source/${it.name}/$MESSAGES_ROOT_NAME")
            val task = project.tasks.register(it.getTaskName("generate", "Localization"), LocalizationGenerate::class.java) { loc ->
                loc.resourceBundleSources.set(messagesFileBasedir)
                loc.templateFile.set(extension.templateFile)
                loc.generatedSourcesOut.set(outDir)
            }
            parentTask.configure {t ->
                t.dependsOn(task)
            }

            it.resources.srcDir(messagesFileBasedir)
            when (extension.templateType) {
                TemplateType.KOTLIN -> {
                    project.extensions.getByType(KotlinSourceSetContainer::class.java).sourceSets.getByName(it.name).apply {
                        kotlin.srcDir(task.map { t -> t.generatedSourcesOut })
                        project.tasks.named(it.getTaskName("compile", "Kotlin")) {
                            it.dependsOn(task)
                        }
                    }
                }
                TemplateType.JAVA -> {
                    it.java.srcDir(task.map { t -> t.generatedSourcesOut })
                    project.tasks.named(it.compileJavaTaskName) {
                        it.dependsOn(task)
                    }
                }
                TemplateType.OTHER -> {
                    // no-op
                }
            }

            it.resources.srcDir(task.map { t -> t.resourceBundleSources })
        }
    }
}

