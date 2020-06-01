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
 */

package ca.stellardrift.permissionsex.backend.conversion.ultraperms

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64
import java.util.concurrent.Callable
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.kotlin.get
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader

class UltraPermissionsFile(val path: Path) {
    val loader: ConfigurationLoader<ConfigurationNode> = YAMLConfigurationLoader.builder().setPath(path).build()

    var node: ConfigurationNode = loader.load()

    fun reload() {
        node = loader.load()
    }

    fun save() {
        loader.save(node)
    }

    private fun ConfigurationNode.getUnwrappedBase64Value(): ConfigurationNode {
        if (isList || isMap) {
            return this
        }

        val value = string?.run {
            Base64.getDecoder().decode(this)
        } ?: return this

        val jsonLoader = GsonConfigurationLoader.builder().apply {
            source = Callable { BufferedReader(InputStreamReader(ByteArrayInputStream(value), StandardCharsets.UTF_8)) }
        }.build()
        return try {
            jsonLoader.load()
        } catch (e: IOException) {
            println("Failed to deserialize entry $key: ${e.message}")
            e.printStackTrace()
            ConfigurationNode.root().setValue(String(value, StandardCharsets.UTF_8))
        }
    }

    private fun ConfigurationNode.setWrappedBase64Value(value: ConfigurationNode) {
        val out = ByteArrayOutputStream(128)
        val jsonLoader = GsonConfigurationLoader.builder().apply {
            sink = Callable { BufferedWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)) }
        }.build()
        jsonLoader.save(value)

        this.value = Base64.getEncoder().encodeToString(out.toByteArray())
    }

    operator fun get(key: String): ConfigurationNode {
        return node[key].getUnwrappedBase64Value()
    }

    operator fun set(key: String, value: ConfigurationNode) {
        node[key].setWrappedBase64Value(value)
    }

    fun unwrapFile() {
        if (!node.isMap) {
            return
        }

        node.childrenMap.forEach { (_, v) ->
            v.value = v.getUnwrappedBase64Value()
        }
    }
}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) {
        args[0]
    } else {
        println(Paths.get(".").toAbsolutePath())
        print("File: ")
        readLine()!!
    }

    val path = Paths.get(filename)
    val upReader =
        UltraPermissionsFile(path)
    upReader.unwrapFile()
    upReader.save()
    println("File $filename unwrapped!")
}
