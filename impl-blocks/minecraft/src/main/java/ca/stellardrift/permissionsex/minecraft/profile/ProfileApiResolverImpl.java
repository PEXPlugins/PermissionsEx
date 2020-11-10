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

package ca.stellardrift.permissionsex.minecraft.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

class ProfileApiResolverImpl implements ProfileApiResolver {

    private static final String USER_AGENT = "PermissionsEx Resolver/" + ProfileApiResolver.class.getPackage().getImplementationVersion();

    private static final URL PROFILE_QUERY_URL;
    private static final int MAX_REQUEST_SIZE = 100;
    private static final Gson GSON;

    static {
        try {
            PROFILE_QUERY_URL = new URL("https://api.mojang.com/profiles/minecraft");
        } catch (final MalformedURLException ex) {
            throw new ExceptionInInitializerError(ex);
        }

        GSON = new GsonBuilder()
                .registerTypeAdapter(UUID.class, new TypeAdapter<UUID>() {
                    @Override
                    public void write(JsonWriter out, UUID value) throws IOException {
                        out.jsonValue(value.toString().replace("-", ""));
                    }

                    @Override
                    public UUID read(JsonReader in) throws IOException {
                        final String mojangId = in.nextString();
                        final StringBuilder result = new StringBuilder(36)
                                .append(mojangId, 0, 8)
                                .append("-")
                                .append(mojangId, 8, 12)
                                .append("-")
                                .append(mojangId, 12, 16)
                                .append("-")
                                .append(mojangId, 16, 20)
                                .append("-")
                                .append(mojangId, 20, 32);
                        return UUID.fromString(result.toString());
                    }
                }.nullSafe())
                .registerTypeAdapterFactory(new GsonAdaptersMinecraftProfile())
                .create();
    }

    private final Executor executor;

    ProfileApiResolverImpl(Executor executor) {
        this.executor = executor;
    }

    /**
     * Creates a new connection open for input and output.
     *
     * @param endpoint Endpoint to connect to
     * @return new connection
     * @throws IOException if unable to establish the connection
     */
    private HttpURLConnection openConnection(final URL endpoint) throws IOException {
        final URLConnection conn = PROFILE_QUERY_URL.openConnection();
        if (!(conn instanceof HttpURLConnection)) {
            throw new IllegalStateException("Profile connection should be a HttpURLConnection but isn't");
        }

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.addRequestProperty("Content-Type", "application/json");
        conn.addRequestProperty("User-Agent", USER_AGENT);
        conn.connect();
        return (HttpURLConnection) conn;
    }

    @Override
    public Flux<MinecraftProfile> resolveByName(Iterable<String> names) {
        return Flux.fromIterable(names)
                .filter(it -> it.length() <= 16)
                .buffer(MAX_REQUEST_SIZE)
                .concatMap(batch -> Flux.<MinecraftProfile>create(sink -> {
                    try {
                        final HttpURLConnection conn = openConnection(PROFILE_QUERY_URL);
                        try (final OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
                             final JsonWriter json = GSON.newJsonWriter(os)) {
                            json.beginArray();
                            for (final String name : batch) {
                                json.value(name);
                            }
                            json.endArray();
                        }

                        if (conn.getResponseCode() != 200) {
                            try (final InputStreamReader is = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                                final JsonObject json = GSON.fromJson(is, JsonObject.class);
                                sink.error(new IOException(json.toString()));
                            }
                        }

                        try (final InputStreamReader is = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                            GSON.<List<MinecraftProfile>>fromJson(is, TypeToken.getParameterized(List.class, MinecraftProfile.class).getType()).forEach(sink::next);
                        }
                    } catch (final IOException ex) {
                        sink.error(ex);
                    }
                    sink.complete();
                }).publishOn(Schedulers.boundedElastic()));
    }
}
