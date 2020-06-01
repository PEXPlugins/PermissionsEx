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

@file:JvmName("HikariConfig")
package ca.stellardrift.permissionsex.hikariconfig

import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import java.util.function.BiFunction
import java.util.regex.Pattern
import org.h2.engine.ConnectionInfo

private val JDBC_URL_REGEX: Pattern =
    Pattern.compile("(?:jdbc:)?([^:]+):(//)?(?:([^:]+)(?::([^@]+))?@)?(.*)")

/**
 * Map from protocol names to a function that transforms the given JDBC url
 */
private val PATH_CANONICALIZERS: Map<String, BiFunction<Path, String, String>> = mapOf(
    "h2" to BiFunction { baseDir, orig ->
        // Bleh if only h2 had a better way of supplying a base directory... oh well...
        val h2Info = ConnectionInfo(orig)
        if (!h2Info.isPersistent || h2Info.isRemote) {
            return@BiFunction orig
        }

        var url = orig
        if (url.startsWith("file:")) {
            url = orig.substring("file:".length)
        }

        val origPath = Paths.get(url)
        return@BiFunction if (origPath.isAbsolute) {
            origPath.toString()
        } else {
            baseDir.toAbsolutePath().resolve(origPath).toString().replace('\\', '/')
        }
    }
)

/**
 * Properties specific to a certain JDBC protocol, immutable.
 *
 * Protocols are identified by their jdbc driver names.
 */
internal val PROTOCOL_SPECIFIC_PROPS: Map<String, Properties> = {
    val mysqlProps = Properties()
    // Config options based on http://assets.en.oreilly.com/1/event/21/Connector_J%20Performance%20Gems%20Presentation.pdf
    mysqlProps.setProperty("useConfigs", "maxPerformance")
    mapOf(
        "com.mysql.jdbc.Driver" to mysqlProps,
        "org.mariadb.jdbc.Driver" to mysqlProps
    )
}()

@JvmOverloads
fun createHikariDataSource(jdbcUrl: String, baseDir: Path = Paths.get(".")): HikariDataSource {
// Based on Sponge`s code, but without alias handling and caching
    val match = JDBC_URL_REGEX.matcher(jdbcUrl)
    require(match.matches()) { "URL $jdbcUrl is not a valid JDBC URL" }

    val protocol: String = match.group(1)
    val hasSlashes = match.group(2) != null
    val user: String? = match.group(3)
    val pass: String? = match.group(4)
    var serverDatabaseSpecifier: String = match.group(5)
    val derelativizer = PATH_CANONICALIZERS[protocol]
    if (derelativizer != null) {
        serverDatabaseSpecifier = derelativizer.apply(baseDir, serverDatabaseSpecifier)
    }

    val unauthedUrl = "jdbc:$protocol${if (hasSlashes) "://" else ":"}$serverDatabaseSpecifier"
    val driverClass: String = java.sql.DriverManager.getDriver(unauthedUrl).javaClass.canonicalName

    val config = com.zaxxer.hikari.HikariConfig()
    config.username = user
    config.password = pass
    config.driverClassName = driverClass

    // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing for info on pool sizing
    config.maximumPoolSize = (Runtime.getRuntime().availableProcessors() * 2) + 1
    val driverSpecificProperties = PROTOCOL_SPECIFIC_PROPS[driverClass]
    val dsProps = if (driverSpecificProperties == null) Properties() else Properties(driverSpecificProperties)
    dsProps.setProperty("baseDir", baseDir.toAbsolutePath().toString())
    config.dataSourceProperties = dsProps
    config.jdbcUrl = unauthedUrl
    return HikariDataSource(config)
}
