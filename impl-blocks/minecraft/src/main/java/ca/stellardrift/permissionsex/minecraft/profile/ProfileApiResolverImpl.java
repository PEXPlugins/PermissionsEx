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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.leangen.geantyref.TypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        final URLConnection conn = endpoint.openConnection();
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
    public Stream<MinecraftProfile> resolveByName(Iterable<String> names) {
        // Filter any names from `names` that are > 16 characters long
        // Split `names` into groups of MAX_REQUEST_SIZE
        final Iterable<Set<String>> batchedNames = batchedAndFiltered(names, MAX_REQUEST_SIZE, it -> it.length() <= 16);

        // For each batch, submit a request in a CompletableFuture
        final Set<CompletableFuture<List<MinecraftProfile>>> requests = new HashSet<>();
        for (final Set<String> batch : batchedNames) {
            requests.add(sendProfileRequest(batch));
        }

        // Create an stream from an iterator that will unwrap the futures
        return unwrapToStream(requests)
            .flatMap(List::stream);
    }

    /**
     * Batches an input lazily.
     *
     * <p>Lazily computes eagerly resolved sets (so, each individual subset will be fully evaluated, but the outer
     * Iterable will be lazy)</p>
     *
     * @param input input values
     * @param batchSize the maximum batch size
     * @param filter the test to filter names
     * @param <T> element type
     * @return an iterable of groups. if no entries match, will have no values
     */
    private static <T> Iterable<Set<T>> batchedAndFiltered(final Iterable<T> input, final int batchSize, final Predicate<T> filter) {
        return () -> new Iterator<Set<T>>() {
            final Iterator<T> base = input.iterator();

            @Override
            public boolean hasNext() {
                return this.base.hasNext();
            }

            @Override
            public Set<T> next() {
                if (!this.base.hasNext()) {
                    throw new NoSuchElementException();
                }
                final Set<T> out = new HashSet<>();
                int counter = 0;
                while (base.hasNext() && counter < batchSize) {
                    final T next = this.base.next();
                    // TODO: handle if no names pass filter
                    if (filter.test(next)) {
                        counter++;
                        out.add(next);
                    }
                }

                return out;
            }
        };
    }

    private CompletableFuture<List<MinecraftProfile>> sendProfileRequest(final Set<String> names) {
        final CompletableFuture<List<MinecraftProfile>> result = new CompletableFuture<>();
        if (names.isEmpty()) {
            result.complete(Collections.emptyList());
            return result;
        }

        this.executor.execute(() -> {
            try {
                final HttpURLConnection conn = openConnection(PROFILE_QUERY_URL);
                try (final OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
                     final JsonWriter json = GSON.newJsonWriter(os)) {
                    json.beginArray();
                    for (final String name : names) {
                        json.value(name);
                    }
                    json.endArray();
                }

                if (conn.getResponseCode() != 200) {
                    try (final InputStreamReader is = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
                        final JsonObject json = GSON.fromJson(is, JsonObject.class);
                        result.completeExceptionally(new IOException(json.toString()));
                        return;
                    }
                }

                try (final InputStreamReader is = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    result.complete(GSON.fromJson(is, TypeFactory.parameterizedClass(List.class, MinecraftProfile.class)));
                }
            } catch (final IOException ex) {
                result.completeExceptionally(ex);
            }
        });
        return result;
    }

    private static <T> Stream<T> unwrapToStream(final Collection<CompletableFuture<T>> futures) {
        return StreamSupport.stream(new MappingSpliterator<>(futures.spliterator()), false);
    }

    private static class MappingSpliterator<V> implements Spliterator<V> {
        private final Spliterator<CompletableFuture<V>> base;

        private MappingSpliterator(final Spliterator<CompletableFuture<V>> base) {
            this.base = base;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super V> action) {
            return this.base.tryAdvance(future -> action.accept(future.join()));
        }

        @Override
        public @Nullable Spliterator<V> trySplit() {
            final @Nullable Spliterator<CompletableFuture<V>> base = this.base.trySplit();
            return base == null ? null : new MappingSpliterator<>(base);
        }

        @Override
        public long estimateSize() {
            return this.base.estimateSize();
        }

        @Override
        public int characteristics() {
            return (this.base.characteristics() | Spliterator.IMMUTABLE) & ~Spliterator.DISTINCT;
        }

    }
}
