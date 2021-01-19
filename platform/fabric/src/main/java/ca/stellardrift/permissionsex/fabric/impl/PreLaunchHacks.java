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
package ca.stellardrift.permissionsex.fabric.impl;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Via i509VCB, a trick to get Brig onto the Knot classpath in order to properly mix in.
 *
 *
 * YOU SHOULD ONLY USE THIS CLASS DURING "preLaunch" and ONLY TARGET A CLASS WHICH IS NOT ANY CLASS YOU MIXIN TO.
 *
 * This will likely not work on Gson because FabricLoader has some special logic related to Gson.
 *
 * Original on GitHub at [i509VCB/Fabric-Junkkyard](https://github.com/i509VCB/Fabric-Junkkyard/blob/ce278daa93804697c745a51af06ec812896ec2ad/src/main/java/me/i509/junkkyard/hacks/PreLaunchHacks.java)
 */
final class PreLaunchHacks {
    private static final ClassLoader KNOT_CLASSLOADER = Thread.currentThread().getContextClassLoader();
    private static final Method ADD_URL_METHOD;

    static {
        try {
            ADD_URL_METHOD = KNOT_CLASSLOADER.getClass().getMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (final NoSuchMethodException ex) {
            throw new RuntimeException("Failed to load Classloader fields", ex);
        }
    }

    /**
     * Hackily load the package which a mixin may exist within.
     *
     * YOU SHOULD NOT TARGET A CLASS WHICH YOU MIXIN TO.
     *
     * @param pathOfAClass The path of any class within the package.
     * @throws ClassNotFoundException if an unknown class name is used
     * @throws InvocationTargetException if an error occurs while injecting
     * @throws IllegalAccessException if an error occurs while injecting
     */
    static void hackilyLoadForMixin(final String pathOfAClass) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        final String resourceName = pathOfAClass.replace(".", "/") + ".class";
        final @Nullable URL fileUrl = PreLaunchHacks.class.getClassLoader().getResource(resourceName);
        if (fileUrl == null) {
            throw new IllegalArgumentException("Could not find URL for class " + pathOfAClass);
        }
        final URLConnection conn;
        try {
            conn = fileUrl.openConnection();
            if (!(conn instanceof JarURLConnection)) {
                throw new IllegalStateException("Expected JAR url connection but got " + conn.getClass());
            }
            ADD_URL_METHOD.invoke(KNOT_CLASSLOADER, ((JarURLConnection) conn).getJarFileURL());
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
