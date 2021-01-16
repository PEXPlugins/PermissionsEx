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
package ca.stellardrift.permissionsex.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;

/**
 * A typesafe provider for translatable messages.
 *
 * <p>Designed for use from generated code containing translation keys.</p>
 *
 * @since 2.0.0
 */
public final class TranslatableProvider implements ComponentLike {
    private static final Logger LOGGER = LoggerFactory.getLogger("PermissionsEx Translations");
    private static final String EXPECTED_EXTENSION = ".properties";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static @Nullable Path lastCodeSource;
    private static @Nullable Set<String> lastKnownResourceBundles;

    private final String key;

    /**
     * Create a new translatable provider
     * @param bundle the name of the {@link java.util.ResourceBundle} containing the message
     * @param key the translation key
     */
    public TranslatableProvider(final String bundle, final String key) {
        requireNonNull(bundle, "bundle");
        requireNonNull(key, "key");
        this.key = bundle + '/' + key;
    }

    /**
     * The translation key used for lookup.
     *
     * @return the translation key
     * @since 2.0.0
     */
    public String key() {
        return this.key;
    }

    /**
     * Create a translatable component with the provided arguments.
     *
     * @param args the arguments
     * @return a new translatable component
     * @since 2.0.0
     */
    public TranslatableComponent tr(final Object... args) {
        return Component.translatable(this.key, this.transformArray(args));
    }

    /**
     * Create a translatable component builder configured with the provided arguments.
     *
     * @param args the arguments
     * @return a new builder
     * @since 2.0.0
     */
    public TranslatableComponent.Builder bTr(final Object... args) {
        return Component.translatable().key(this.key).args(this.transformArray(args));
    }

    private Component[] transformArray(final Object[] input) {
        final Component[] output = new Component[input.length];
        for (int i = 0, length = input.length; i < length; ++i) {
            output[i] = asComponent(input[i]);
        }
        return output;
    }

    private Component asComponent(final Object input) {
        if (input instanceof Component) {
            return (Component) input;
        } else if (input instanceof ComponentLike) {
            return ((ComponentLike) input).asComponent();
        } else {
            return Component.text(String.valueOf(input));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This will create a component without any arguments.</p>
     */
    @Override
    public @NonNull Component asComponent() {
        return this.tr();
    }

    /**
     * Get known locales for a certain bundle name.
     *
     * @param loaderOf The class to use to determine a code source
     * @param bundleName the name of the bundle to find languages of
     * @return the known locales
     */
    public static synchronized Stream<Locale> knownLocales(final Class<?> loaderOf, final String bundleName) {
        // Get all resources from the same code source as the class
        // If the code source is a jar: open the jar, iterate through entries?
        try {
            URL sourceUrl = loaderOf.getProtectionDomain().getCodeSource().getLocation();
            // Some class loaders give the full url to the class, some give the URL to its jar.
            // We want the containing jar, so we will unwrap jar-schema code sources.
            if (sourceUrl.getProtocol().equals("jar")) {
                final int exclamationIdx = sourceUrl.getPath().lastIndexOf('!');
                if (exclamationIdx != -1) {
                    sourceUrl = new URL(sourceUrl.getPath().substring(0, exclamationIdx));
                }
            }
            final Path codeSource = Paths.get(sourceUrl.toURI());
            final String bundlePathName = bundleName.replace('.', '/');
            final Set<String> paths;
            if (!codeSource.equals(lastCodeSource)) {
                // If the code source is a directory: visit the path
                final String fileName = codeSource.getFileName().toString();
                if (Files.isDirectory(codeSource)) {
                    try (final Stream<Path> files = Files.walk(codeSource)) {
                        paths = files.filter(it -> it.endsWith(EXPECTED_EXTENSION)) // only properties files
                            .map(it -> it.relativize(codeSource)) // relative path
                            .map(it -> it.toString().replace('\\', '/')) // with consistent slashes
                            .collect(Collectors.toSet());
                    }
                } else if (fileName.endsWith("jar") || fileName.endsWith("zip")) {
                    // read from the archive
                    paths = new HashSet<>();
                    try (final ZipInputStream is = new ZipInputStream(Files.newInputStream(codeSource))) {
                        @Nullable ZipEntry entry;
                        while ((entry = is.getNextEntry()) != null) {
                            if (entry.getName().endsWith(EXPECTED_EXTENSION)) {
                                paths.add(entry.getName());
                            }
                        }
                    }
                } else {
                    throw new IOException("Unknown file type " + codeSource);
                }
                lastCodeSource = codeSource;
                lastKnownResourceBundles = Collections.unmodifiableSet(paths);
            } else {
                paths = requireNonNull(lastKnownResourceBundles, "has been set");
            }

            return paths.stream()
                .filter(it -> it.startsWith(bundlePathName))
                .map(it -> it.substring(bundlePathName.length(), it.length() - EXPECTED_EXTENSION.length()))
                .filter(it -> it.isEmpty() || it.startsWith("_")) // either default file, or for a language
                .map(it -> {
                    if (it.isEmpty()) {
                        return DEFAULT_LOCALE;
                    } else {
                        return Translator.parseLocale(it.substring(1));
                    }
                });
        } catch (final Exception ex) {
            LOGGER.error("Failed to read known locales for bundle {}", bundleName, ex);
        }

        // Fallback
        return Stream.of(DEFAULT_LOCALE);
    }

    public static void registerAllTranslations(final String bundleName, final Stream<Locale> languages, final ClassLoader loader) {
        final TranslationRegistry registry = TranslationRegistry.create(Key.key("permissionsex", bundleName.toLowerCase(Locale.ROOT)));
        registry.defaultLocale(DEFAULT_LOCALE);

        languages.forEach(language  -> {
            final ResourceBundle bundle = ResourceBundle.getBundle(
                bundleName,
                language,
                loader,
                UTF8ResourceBundleControl.get()
            );

            for (final String key : bundle.keySet()) {
                try {
                    registry.register(formatKey(bundleName, key), language, new MessageFormat(bundle.getString(key)));
                } catch (final Exception ex) {
                    LOGGER.warn("Failed to register translation key {} in bundle {}", key, bundleName, ex);
                }
            }
        });

        GlobalTranslator.get().addSource(registry);
    }

    private static String formatKey(final String bundleName, final String key) {
        return bundleName + '/' + key;
    }
}
