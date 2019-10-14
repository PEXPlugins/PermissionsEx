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

package ca.stellardrift.permissionsex.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.flux
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

interface MinecraftProfile {
    val name: String
    val uuid: UUID
}

data class MinecraftProfileImpl(override val name: String, @SerializedName("id") override val uuid: UUID) : MinecraftProfile

private val PROFILE_QUERY_URL = URL("https://api.mojang.com/profiles/minecraft")
private const val MAX_REQUEST_SIZE = 100
private val gson: Gson = GsonBuilder().apply {
    registerTypeAdapter(UUID::class.java, object : TypeAdapter<UUID>() {
        override fun write(out: JsonWriter?, value: UUID?) {
            out?.jsonValue(value?.toString()?.replace("-", ""))
        }

        override fun read(`in`: JsonReader?): UUID {
            val mojangId = `in`?.nextString()!!
            return UUID.fromString("${mojangId.substring(0, 8)}-${mojangId.substring(8, 12)}" +
                    "-${mojangId.substring(12, 16)}-${mojangId.substring(16, 20)}-${mojangId.substring(20, 32)}")
        }

    })
}.create()

@ExperimentalCoroutinesApi
fun resolveMinecraftProfile(names: Flux<String>, cacheLookup: suspend (name: String) -> MinecraftProfile? = { null }): Flux<MinecraftProfile> {
    try {
        return names.buffer(MAX_REQUEST_SIZE)
            .flatMap {
                flux<MinecraftProfile>(Dispatchers.IO) {
                    val conn = PROFILE_QUERY_URL.openConnection()
                    check(conn is HttpURLConnection) { "Profile connection should be a HttpURLConnection but isn't" }

                    conn.doInput = true
                    conn.doOutput = true
                    conn.addRequestProperty("Content-Type", "application/json")
                    conn.connect()

                    OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                        gson.newJsonWriter(os).use { json ->
                            json.beginArray()
                            it.forEach {
                                val potentialResult = cacheLookup(it)
                                if (potentialResult != null) {
                                    send(potentialResult)
                                } else {
                                    json.value(it)
                                }
                            }
                            json.endArray()
                        }
                    }

                    InputStreamReader(conn.inputStream, "UTF-8").use { inp ->
                        gson.newJsonReader(inp).use {
                            it.beginArray()
                            while (it.hasNext() && it.peek() == JsonToken.BEGIN_OBJECT) {
                                send(gson.fromJson<MinecraftProfile>(it, MinecraftProfileImpl::class.java))
                            }
                            it.endArray()
                        }
                    }
                }
            }
    } catch (ex : Exception) {
        return ex.toFlux()
    }
}
