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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * A typesafe provider for translatable messages.
 *
 * <p>Designed for use from generated code containing translation keys.</p>
 *
 * @since 2.0.0
 */
public final class TranslatableProvider implements ComponentLike {
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
}
