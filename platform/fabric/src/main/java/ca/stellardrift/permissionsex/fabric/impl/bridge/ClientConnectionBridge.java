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
package ca.stellardrift.permissionsex.fabric.impl.bridge;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface ClientConnectionBridge {

    /**
     * The hostname a client used to connect to this server.
     *
     * <p>May be <em>unresolved</em> if the provided hostname could not be resolved.</p>
     */
    @Nullable InetSocketAddress virtualHost();

    void virtualHost(InetSocketAddress address);

}
