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
package ca.stellardrift.permissionsex.impl;

import ca.stellardrift.permissionsex.util.TranslatableProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslatableProviderClasspathTest {

    // A simple test to ensure we are still loading translations from the classpath properly
    @Test
    void testBundleDiscovery() {
        final TranslatableProvider message = Messages.CONFIG_ERROR_SAVE;

        assertEquals(
            Component.text("Unable to write permissions configuration"),
            GlobalTranslator.render(message.tr(), Locale.ENGLISH)
        );
    }

    @Test
    void testLanguageFallback() {
        final TranslatableProvider message = Messages.CONFIG_ERROR_SAVE;

        assertEquals(
            Component.text("Unable to write permissions configuration"),
            GlobalTranslator.render(message.tr(), new Locale("ab", "cd"))
        );

    }

}
