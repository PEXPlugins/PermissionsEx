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

package ca.stellardrift.permissionsex.sql.hikari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.h2.engine.ConnectionInfo;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class Hikari {
    private static final Pattern JDBC_URL_REGEX = Pattern.compile("(?:jdbc:)?([^:]+):(//)?(?:([^:]+)(?::([^@]+))?@)?(.*)");

    /**
     * Map from protocol names to a function that transforms the given JDBC url
     */
    private static final PMap<String, BiFunction<Path, String, String>> PATH_CANONICALIZERS = HashTreePMap.singleton("h2", (baseDir, orig) ->{
        // Bleh if only h2 had a better way of supplying a base directory... oh well...
        final ConnectionInfo h2Info = new ConnectionInfo(orig);
        if (!h2Info.isPersistent() || h2Info.isRemote()) {
            return orig;
        }

        String url = orig;
        if (url.startsWith("file:")) {
            url = orig.substring("file:".length());
        }

        final Path origPath = Paths.get(url);
        if (origPath.isAbsolute()) {
            return origPath.toString();
        } else {
            return baseDir.toAbsolutePath().resolve(origPath).toString().replace('\\', '/');
        }
    });

    /**
     * Properties specific to a certain JDBC protocol, immutable.
     *
     * Protocols are identified by their jdbc driver names.
     */
    private static final PMap<String, Properties> PROTOCOL_SPECIFIC_PROPS;

    static {
        final Properties mysqlProps = new Properties();
        // Config options based on http://assets.en.oreilly.com/1/event/21/Connector_J%20Performance%20Gems%20Presentation.pdf
        mysqlProps.setProperty("useConfigs", "maxPerformance");

        PROTOCOL_SPECIFIC_PROPS = HashTreePMap.<String, Properties>empty()
                .plus("com.mysql.jdbc.Driver", mysqlProps)
                .plus("org.maridadb.jdbc.Driver", mysqlProps);
    }

    /**
     * Create a data source for the provided URL, relative to the current working directory.
     *
     * @param jdbcUrl URL to connect to
     * @return a new data source
     * @throws SQLException if unable to resolve a driver for the URL
     * @since 2.0.0
     */
    public static HikariDataSource createDataSource(final String jdbcUrl) throws SQLException {
        return createDataSource(jdbcUrl, Paths.get("."));
    }

    /**
     * Create a data source for the provided {@code jdbcUrl}, with any filesystem
     * paths made relative to {@code baseDir}.
     *
     * @param jdbcUrl URL to connect to
     * @param baseDir base directory
     * @return a new data source
     * @throws SQLException if unable to resolve a driver for the URL
     * @since 2.0.0
     */
    public static HikariDataSource createDataSource(final String jdbcUrl, final Path baseDir) throws SQLException {
        // Based on Sponge`s code, but without alias handling and caching
        final Matcher match = JDBC_URL_REGEX.matcher(requireNonNull(jdbcUrl, "jdbcUrl"));
        if (!match.matches()) {
            throw new IllegalArgumentException("URL " + jdbcUrl + " is not a valid JDBC URL");
        }

        final String protocol = match.group(1);
        final boolean hasSlashes = match.group(2) != null;
        final @Nullable String user = match.group(3);
        final @Nullable String pass = match.group(4);
        String serverDatabaseSpecifier = match.group(5);
        final BiFunction<Path, String, String> derelativizer = PATH_CANONICALIZERS.get(protocol);
        if (derelativizer != null) {
            serverDatabaseSpecifier = derelativizer.apply(baseDir, serverDatabaseSpecifier);
        }

        final String unauthedUrl = new StringBuilder("jdbc:")
                .append(protocol)
                .append(hasSlashes ? "://" : ":")
                .append(serverDatabaseSpecifier)
                .toString();
        final String driverClass = DriverManager.getDriver(unauthedUrl).getClass().getCanonicalName();

        final HikariConfig config = new HikariConfig();
        config.setUsername(user);
        config.setPassword(pass);
        config.setDriverClassName(driverClass);

        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing for info on pool sizing
        config.setMaximumPoolSize((Runtime.getRuntime().availableProcessors() * 2) + 1);
        final @Nullable Properties driverSpecificProperties = PROTOCOL_SPECIFIC_PROPS.get(driverClass);
        final Properties dsProps;
        if (driverSpecificProperties == null) {
            dsProps = new Properties();
        } else {
            dsProps = new Properties(driverSpecificProperties);
        }
        dsProps.setProperty("baseDir", baseDir.toAbsolutePath().toString());
        config.setDataSourceProperties(dsProps);
        config.setJdbcUrl(unauthedUrl);
        return new HikariDataSource(config);
    }

    private Hikari() {}
}
