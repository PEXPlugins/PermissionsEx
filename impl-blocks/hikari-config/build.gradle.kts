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
 *
 */
plugins {
    id("pex-component")
}

useCheckerFramework()
dependencies {
    val h2Version: String by project
    api("org.pcollections:pcollections:3.1.4")
    api("com.zaxxer:HikariCP:4.0.1")
    compileOnly("com.h2database:h2:$h2Version")
}
