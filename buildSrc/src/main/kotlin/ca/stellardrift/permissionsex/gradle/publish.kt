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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.net.URI

fun <T> act(func: T.() -> Unit): Action<T> {
   return Action {
      it.func()
   }
}


fun Project.setupPublication() {
   plugins.apply(MavenPublishPlugin::class.java)
   plugins.apply(SigningPlugin::class.java)

   // Setup publication
   extensions.configure(PublishingExtension::class.java, act {
      publications(act {
         val mavenPub = register("maven", MavenPublication::class.java, act {
            from(project.components.getByName("java"))
            pom.apply {
               val scmUrl = "github.com/PEXPlugins/PermissionsEx"
               name.set(project.name)
               description.set(project.property("pexDescription").toString())
               url.set("https://$scmUrl")

               licenses {
                  it.license { l ->
                     l.name.set("Apache 2.0")
                     l.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                  }
               }
               scm(act {
                  connection.set("scm:git:https://${scmUrl}.git")
                  developerConnection.set("scm:git:ssh://$scmUrl.git")
                  url.set("https://$scmUrl")
               })

               issueManagement(act {
                  system.set("GitHub")
                  url.set("https://$scmUrl/issues")
               })

               ciManagement(act {
                  system.set("Jenkins")
                  url.set("https://ci.yawk.at/job/PermissionsEx")
               })
            }
         })
         repositories(act {
            if (project.hasProperty("pexUsername") && project.hasProperty("pexPassword")) {
               maven {
                  it.name = "pex"
                  it.url = URI("https://repo.glaremasters.me/repository/permissionsex")
                  it.credentials {
                     it.username = project.property("pexUsername").toString()
                     it.password = project.property("pexPassword").toString()
                  }
               }
            }
         })

         // Setup signing
         project.extensions.configure(SigningExtension::class.java, act {
            useGpgCmd()
            sign(project.configurations.getByName("archives"))
            sign(mavenPub.get())
         })
      })

   })

   tasks.withType(Sign::class.java) {
      it.onlyIf {
         project.hasProperty("forceSign") || !(project.version as String).endsWith("-SNAPSHOT")
      }
   }
}

fun Project.setupJavadocSourcesJars() {
   val javadocJar = tasks.create("javadocJar", Jar::class.java) {
      it.archiveClassifier.set("javadoc")
      it.from((tasks.getByName("javadoc") as Javadoc).destinationDir)
   }

   val sourcesJar = tasks.create("sourcesJar", Jar::class.java) {
      it.archiveClassifier.set("sources")
      it.from(project.convention.getPlugin(JavaPluginConvention::class.java)
         .sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get().allSource)
   }

   extensions.configure(PublishingExtension::class.java) {
      it.publications.forEach {pub ->
         if (pub is MavenPublication) {
            pub.artifact(javadocJar)
            pub.artifact(sourcesJar)
         }
      }
   }
}