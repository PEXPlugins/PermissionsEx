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

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * Load Brig and Authlib for mixin purposes
 */
public class PreLaunchInjector implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreLaunchInjector.class);

    @Override
    public void onPreLaunch() {
        /*try {
            PreLaunchHacks.hackilyLoadForMixin("com.mojang.brigadier.Message");
            PreLaunchHacks.hackilyLoadForMixin("cloud.commandframework.brigadier.BrigadierManagerHolder");
        } catch (final ClassNotFoundException | InvocationTargetException | IllegalAccessException ex) {
            LOGGER.warn("Failed to inject Brigadier into transformation path, Vanilla command permissions may not work.", ex);
        }*/
    }

}
