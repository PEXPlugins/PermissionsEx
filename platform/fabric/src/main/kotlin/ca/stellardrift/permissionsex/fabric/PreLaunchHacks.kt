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

package ca.stellardrift.permissionsex.fabric

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.URL

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
object PreLaunchHacks {
    private val KNOT_CLASSLOADER = Thread.currentThread().contextClassLoader
    private var ADD_URL_METHOD: Method

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
    @Throws(ClassNotFoundException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun hackilyLoadForMixin(pathOfAClass: String?) {
        val url = Class.forName(pathOfAClass).protectionDomain.codeSource.location
        ADD_URL_METHOD.invoke(KNOT_CLASSLOADER, url)
    }

    init {
        try {
            ADD_URL_METHOD = KNOT_CLASSLOADER.javaClass.getMethod("addURL", URL::class.java)
            ADD_URL_METHOD.isAccessible = true
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException("Failed to load Classloader fields", e)
        }
    }
}
