/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.backends.hybrid;

import org.apache.commons.dbcp.BasicDataSource;
import org.bukkit.configuration.ConfigurationSection;

import com.google.common.collect.ImmutableSet;

import ru.tehkode.permissions.PermissionsData;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.SchemaUpdate;
import ru.tehkode.permissions.backends.caching.CachingGroupData;
import ru.tehkode.permissions.backends.caching.CachingUserData;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author code
 */
public class HybridBackend extends PermissionBackend {
    public final static char PATH_SEPARATOR = '/';
    public FileConfig permissions;
    public File permissionsFile;
    private final Map<String, List<String>> worldInheritanceCache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final AtomicReference<ImmutableSet<String>> userNamesCache = new AtomicReference<>();
    private Map<String, Object> tableNames;
    private SQLQueryCache queryCache;
    private static final SQLQueryCache DEFAULT_QUERY_CACHE;

    static {
        try {
            DEFAULT_QUERY_CACHE = new SQLQueryCache(HybridBackend.class.getResourceAsStream("/sql/default/queries.properties"), null);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private BasicDataSource ds;
    protected String dbDriver;

    public HybridBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
        super(manager, config);
        fileBackend(manager, config);
        sqlBackend(manager, config);
    }

    private void sqlBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
        final String dbUri = getConfig().getString("uri", "");
        final String dbUser = getConfig().getString("user", "");
        final String dbPassword = getConfig().getString("password", "");

        if (dbUri == null || dbUri.isEmpty()) {
            getConfig().set("uri", "mysql://localhost/exampledb");
            getConfig().set("user", "databaseuser");
            getConfig().set("password", "databasepassword");
            manager.getConfiguration().save();
            throw new PermissionBackendException("SQL connection is not configured, see config.yml");
        }
        dbDriver = dbUri.split(":", 2)[0];

        this.ds = new BasicDataSource();
        String driverClass = getDriverClass(dbDriver);
        if (driverClass != null) {
            this.ds.setDriverClassName(driverClass);
        }
        this.ds.setUrl("jdbc:" + dbUri);
        this.ds.setUsername(dbUser);
        this.ds.setPassword(dbPassword);
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        this.ds.setMaxActive((Runtime.getRuntime().availableProcessors() * 2) + 1);
        this.ds.setMaxWait(200); // 4 ticks
        this.ds.setValidationQuery("SELECT 1 AS dbcp_validate");
        this.ds.setTestOnBorrow(true);

        InputStream queryLocation = getClass().getResourceAsStream("/sql/" + dbDriver + "/queries.properties");
        if (queryLocation != null) {
            try {
                this.queryCache = new SQLQueryCache(queryLocation, DEFAULT_QUERY_CACHE);
            } catch (IOException e) {
                throw new PermissionBackendException("Unable to access database-specific queries", e);
            }
        } else {
            this.queryCache = DEFAULT_QUERY_CACHE;
        }
        try (SQLConnection conn = getSQL()) {
            conn.checkConnection();
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause() instanceof Exception) {
                e = (Exception) e.getCause();
            }
            throw new PermissionBackendException("Unable to connect to SQL database", e);
        }

        getManager().getLogger().info("Successfully connected to SQL database");

        addSchemaUpdate(new SchemaUpdate(2) {
            @Override
            public void performUpdate() throws PermissionBackendException {
                // Change encoding for all columns to utf8mb4
                // Change collation for all columns to utf8mb4_general_ci
                try (SQLConnection conn = getSQL()) {
                    conn.prep("ALTER TABLE `{permissions}` DROP KEY `unique`, MODIFY COLUMN `permission` TEXT NOT NULL").execute();
                } catch (SQLException | IOException e) {
                    throw new PermissionBackendException(e);
                }
            }
        });
        addSchemaUpdate(new SchemaUpdate(1) {
            @Override
            public void performUpdate() throws PermissionBackendException {
                try (SQLConnection conn = getSQL()) {
                    PreparedStatement updateStmt = conn.prep("entity.options.add");
                    ResultSet res = conn.prepAndBind("SELECT `name`, `type` FROM `{permissions_entity}` WHERE `default`='1'").executeQuery();
                    while (res.next()) {
                            conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "default", "", "true");
                            updateStmt.addBatch();
                    }
                    updateStmt.executeBatch();

                    // Update tables
                    conn.prep("ALTER TABLE `{permissions_entity}` DROP COLUMN `default`").execute();
                } catch (SQLException | IOException e) {
                    throw new PermissionBackendException(e);
                }
            }
        });
        addSchemaUpdate(new SchemaUpdate(0) {
            @Override
            public void performUpdate() throws PermissionBackendException {
                try (SQLConnection conn = getSQL()) {
                    // TODO: Table modifications not supported in SQLite
                    // Prefix/sufix -> options
                    PreparedStatement updateStmt = conn.prep("entity.options.add");
                    ResultSet res = conn.prepAndBind("SELECT `name`, `type`, `prefix`, `suffix` FROM `{permissions_entity}` WHERE LENGTH(`prefix`)>0 OR LENGTH(`suffix`)>0").executeQuery();
                    while (res.next()) {
                        String prefix = res.getString("prefix");
                        if (!prefix.isEmpty() && !prefix.equals("null")) {
                            conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "prefix", "", prefix);
                            updateStmt.addBatch();
                        }
                        String suffix = res.getString("suffix");
                        if (!suffix.isEmpty() && !suffix.equals("null")) {
                            conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "suffix", "", suffix);
                            updateStmt.addBatch();
                        }
                    }
                    updateStmt.executeBatch();

                    // Data type corrections

                    // Update tables
                    conn.prep("ALTER TABLE `{permissions_entity}` DROP KEY `name`").execute();
                    conn.prep("ALTER TABLE `{permissions_entity}` DROP COLUMN `prefix`, DROP COLUMN `suffix`").execute();
                    conn.prep("ALTER TABLE `{permissions_entity}` ADD CONSTRAINT UNIQUE KEY `name` (`name`, `type`)").execute();

                    conn.prep("ALTER TABLE `{permissions}` DROP KEY `unique`").execute();
                    conn.prep("ALTER TABLE `{permissions}` ADD CONSTRAINT UNIQUE `unique` (`name`,`permission`,`world`,`type`)").execute();
                } catch (SQLException | IOException e) {
                    throw new PermissionBackendException(e);
                }
            }
        });
        this.setupAliases();
        this.deployTables();
        performSchemaUpdate();

        try (SQLConnection conn = getSQL()) {
            conn.prep("ALTER TABLE `{permissions}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
            conn.prep("ALTER TABLE `{permissions_entity}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
            conn.prep("ALTER TABLE `{permissions_inheritance}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
        } catch (SQLException | IOException e) {
            // Ignore, this MySQL version just doesn't support it.
        }
    }

    SQLQueryCache getQueryCache() {
        return queryCache;
    }

    protected static String getDriverClass(String alias) {
        if (alias.equals("mysql")) {
            return "com.mysql.jdbc.Driver";
        } else if (alias.equals("sqlite")) {
            return "org.sqlite.JDBC";
        } else if (alias.matches("postgres?")) {
            return "org.postgresql.Driver";
        }
        return null;
    }

    public SQLConnection getSQL() throws SQLException {
        if (ds == null) {
            throw new SQLException("SQL connection information was not correct, could not retrieve connection");
        }
        return new SQLConnection(ds.getConnection(), this);
    }

    public String getTableName(String identifier) {
        Map<String, Object> tableNames = this.tableNames;
        if (tableNames == null) {
            return identifier;
        }

        Object ret = tableNames.get(identifier);
        if (ret == null) {
            return identifier;
        }
        return ret.toString();
    }

    @Override
    public PermissionsUserData getUserData(String name) {
        CachingUserData data = new CachingUserData(new HybridUserData(name, HybridUserData.Type.USER, this), getExecutor(), new ReentrantReadWriteLock());
        updateNameCache(userNamesCache, data);
        return data;
    }

    /**
     * Update the cache of names for a newly created data object, if necessary.
     *
     * @param list The pointer to current cache state
     * @param data The data to check for presence
     */
    private void updateNameCache(AtomicReference<ImmutableSet<String>> list, PermissionsData data) {
        ImmutableSet<String> cache, newVal;
        do {
            newVal = cache = list.get();
            if (cache == null || (!cache.contains(data.getIdentifier()) && !data.isVirtual())) {
                newVal = null;
            }

        } while (!list.compareAndSet(cache, newVal));
    }

    /**
     * Clear the names cache for the type of the provided data object
     *
     * @param data The data object that was updated making this necessary.
     */
    void updateNameCache(HybridUserData data) {
        updateNameCache(userNamesCache, data);
    }

    /**
     * Gets the names of known entities of the give type, returning cached values if possible
     *
     * @param cacheRef The cache reference to check
     * @param type The type to get
     * @return A set of known entity names
     */
    private ImmutableSet<String> getEntityNames(AtomicReference<ImmutableSet<String>> cacheRef, HybridUserData.Type type) {
        while (true) {
            ImmutableSet<String> cache = cacheRef.get();
            if (cache != null) {
                return cache;
            } else {
                try (SQLConnection conn = getSQL()) {
                    ImmutableSet<String> newCache = ImmutableSet.copyOf(HybridUserData.getEntitiesNames(conn, type, false));
                    if (cacheRef.compareAndSet(null, newCache)) {
                        return newCache;
                    }
                } catch (SQLException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public boolean hasUser(String userName) {
        try (SQLConnection conn = getSQL()) {
            ResultSet res = conn.prepAndBind("entity.exists", userName, HybridUserData.Type.USER.ordinal()).executeQuery();
            return res.next();
        } catch (SQLException | IOException e) {
            return false;
        }
    }

    @Override
    public Collection<String> getUserIdentifiers() {
        return getEntityNames(userNamesCache, HybridUserData.Type.USER);
    }

    @Override
    public Collection<String> getUserNames() {
        // TODO: Look at implementing caching
        Set<String> ret = new HashSet<>();
        try (SQLConnection conn = getSQL()) {
            ResultSet set = conn.prepAndBind("SELECT `value` FROM `{permissions}` WHERE `type` = ? AND `permission` = 'name' AND `value` IS NOT NULL", HybridUserData.Type.USER.ordinal()).executeQuery();
            while (set.next()) {
                ret.add(set.getString("value"));
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
        return Collections.unmodifiableSet(ret);
    }

    protected final void setupAliases() {
        ConfigurationSection aliases = getConfig().getConfigurationSection("aliases");

        if (aliases == null) {
            return;
        }

        tableNames = aliases.getValues(false);
    }

    private void executeStream(SQLConnection conn, InputStream str) throws SQLException, IOException {
        String deploySQL = StringUtils.readStream(str);


        Statement s = conn.getStatement();

        for (String sqlQuery : deploySQL.trim().split(";")) {
            sqlQuery = sqlQuery.trim();
            if (sqlQuery.isEmpty()) {
                continue;
            }

            sqlQuery = conn.expandQuery(sqlQuery + ";");

            s.addBatch(sqlQuery);
        }
        s.executeBatch();
    }

    protected final void deployTables() throws PermissionBackendException {
        try (SQLConnection conn = getSQL()) {
            if (conn.hasTable("{permissions}") && conn.hasTable("{permissions_entity}") && conn.hasTable("{permissions_inheritance}")) {
                return;
            }
            InputStream databaseDumpStream = getClass().getResourceAsStream("/sql/" + dbDriver + "/deploy.sql");

            if (databaseDumpStream == null) {
                throw new Exception("Can't find appropriate database dump for used database (" + dbDriver + "). Is it bundled?");
            }

            getLogger().info("Deploying default database scheme");
            executeStream(conn, databaseDumpStream);
            setSchemaVersion(getLatestSchemaVersion());
        } catch (Exception e) {
            throw new PermissionBackendException("Deploying of default data failed. Please initialize database manually using " + dbDriver + ".sql", e);
        }

        PermissionsGroupData defGroup = getGroupData("default");
        defGroup.setPermissions(Collections.singletonList("modifyworld.*"), null);
        defGroup.setOption("default", "true", null);
        defGroup.save();

        getLogger().info("Database scheme deploying complete.");
    }

    @Override
    public void close() throws PermissionBackendException {
        super.close();
        if (ds != null) {
            try {
                ds.close();
            } catch (SQLException e) {
                throw new PermissionBackendException("Error while closing", e);
            }
        }
    }

    public void fileBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
        String permissionFilename = getConfig().getString("file");

        // Default settings
        if (permissionFilename == null) {
            permissionFilename = "permissions.yml";
            getConfig().set("file", "permissions.yml");
        }

        String baseDir = manager.getConfiguration().getBasedir();

        if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
            baseDir = baseDir.replace("\\", File.separator);
        }

        File baseDirectory = new File(baseDir);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }

        this.permissionsFile = new File(baseDir, permissionFilename);
        addSchemaUpdate(new SchemaUpdate(1) {
            @Override
            public void performUpdate() {
                ConfigurationSection userSection = permissions.getConfigurationSection("users");
                if (userSection != null) {
                    for (Map.Entry<String, Object> e : userSection.getValues(false).entrySet()) {
                        if (e.getValue() instanceof ConfigurationSection) {
                            allWorlds((ConfigurationSection) e.getValue());
                        }
                    }
                }
                ConfigurationSection groupSection = permissions.getConfigurationSection("groups");
                if (groupSection != null) {
                    for (Map.Entry<String, Object> e : groupSection.getValues(false).entrySet()) {
                        if (e.getValue() instanceof ConfigurationSection) {
                            allWorlds((ConfigurationSection) e.getValue());
                        }
                    }
                }
            }

            private void allWorlds(ConfigurationSection section) {
                singleWorld(section);
                ConfigurationSection worldSection = section.getConfigurationSection("worlds");
                if (worldSection != null) {
                    for (Map.Entry<String, Object> e : worldSection.getValues(false).entrySet()) {
                        if (e.getValue() instanceof ConfigurationSection) {
                            singleWorld((ConfigurationSection) e.getValue());
                        }
                    }
                }
            }

            private void singleWorld(ConfigurationSection section) {
                if (section.isSet("prefix")) {
                    section.set(buildPath("options", "prefix"), section.get("prefix"));
                    section.set("prefix", null);
                }

                if (section.isSet("suffix")) {
                    section.set(buildPath("options", "suffix"), section.get("suffix"));
                    section.set("suffix", null);
                }

                if (section.isSet("default")) {
                    section.set(buildPath("options", "default"), section.get("default"));
                    section.set("default", null);
                }
            }
        });
        reload();
        performSchemaUpdate();
    }

    @Override
    public int getSchemaVersion() {
        lock.readLock().lock();
        try {
            return this.permissions.getInt("schema-version", -1);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected void setSchemaVersion(int version) {
        lock.writeLock().lock();
        try {
            this.permissions.set("schema-version", version);
        } finally {
            lock.writeLock().unlock();
        }
        save();
    }

    @Override
    public List<String> getWorldInheritance(String world) {
        if (world != null && !world.isEmpty()) {
            List<String> parentWorlds = worldInheritanceCache.get(world);
            if (parentWorlds == null) {
                synchronized (lock) {
                    parentWorlds = this.permissions.getStringList(buildPath("worlds", world, "inheritance"));
                    if (parentWorlds != null) {
                        parentWorlds = Collections.unmodifiableList(parentWorlds);
                        worldInheritanceCache.put(world, parentWorlds);
                        return parentWorlds;
                    }
                }
            } else {
                return parentWorlds;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Map<String, List<String>> getAllWorldInheritance() {
        synchronized (lock) {
            ConfigurationSection worldsSection = this.permissions.getConfigurationSection("worlds");
            if (worldsSection == null) {
                return Collections.emptyMap();
            }

            Map<String, List<String>> ret = new HashMap<>();
            for (String world : worldsSection.getKeys(false)) {
                ret.put(world, getWorldInheritance(world));
            }
            return Collections.unmodifiableMap(ret);
        }
    }

    @Override
    public void setWorldInheritance(final String world, List<String> rawParentWorlds) {
        if (world == null || world.isEmpty()) {
            return;
        }
        final List<String> parentWorlds = new ArrayList<>(rawParentWorlds);
        worldInheritanceCache.put(world, parentWorlds);

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    permissions.set(buildPath("worlds", world, "inheritance"), parentWorlds);
                    save();
                }
            }
        });
    }

    private ConfigurationSection getNode(String basePath, String entityName) {
        if (permissions.isLowerCased(basePath)) {
            entityName = entityName.toLowerCase();
        }
        String nodePath = HybridBackend.buildPath(basePath, entityName);
        lock.readLock().lock();
        try {

            ConfigurationSection entityNode = this.permissions.getConfigurationSection(nodePath);

            if (entityNode != null) {
                return entityNode;
            }

            if (!permissions.isLowerCased(basePath)) {
                ConfigurationSection users = this.permissions.getConfigurationSection(basePath);

                if (users != null) {
                    for (Map.Entry<String, Object> entry : users.getValues(false).entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(entityName)
                                && entry.getValue() instanceof ConfigurationSection) {
                            return (ConfigurationSection) entry.getValue();
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            ConfigurationSection section = this.permissions.createSection(nodePath);
            this.permissions.set(nodePath, null);
            return section;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public PermissionsGroupData getGroupData(String groupName) {
        ConfigurationSection section = getNode("groups", groupName);
        final CachingGroupData data = new CachingGroupData(new FileData(section, "inheritance"), getExecutor(), lock);
        data.load();
        return data;
    }

    @Override
    public boolean hasGroup(String group) {
        lock.readLock().lock();
        try {
            if (this.permissions.isConfigurationSection(buildPath("groups", group))) {
                return true;
            }

            ConfigurationSection userSection = this.permissions.getConfigurationSection("groups");
            if (userSection != null) {
                for (String name : userSection.getKeys(false)) {
                    if (group.equalsIgnoreCase(name)) {
                        return true;
                    }
                }

            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<String> getGroupNames() {
        lock.readLock().lock();
        try {
            ConfigurationSection groups = this.permissions.getConfigurationSection("groups");
            return groups != null ? groups.getKeys(false) : Collections.<String> emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    public static String buildPath(String... path) {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        char separator = PATH_SEPARATOR; // permissions.options().pathSeparator();

        for (String node : path) {
            if (node.isEmpty()) {
                continue;
            }

            if (!first) {
                builder.append(separator);
            }

            builder.append(node);

            first = false;
        }

        return builder.toString();
    }

    @Override
    public void reload() throws PermissionBackendException {
        FileConfig newPermissions = new FileConfig(permissionsFile, new Object(), "users");
        newPermissions.options().pathSeparator(PATH_SEPARATOR);
        try {
            newPermissions.load();
            getLogger().info("Permissions file successfully reloaded");
            worldInheritanceCache.clear();
            userNamesCache.set(null);
            this.permissions = newPermissions;
        } catch (FileNotFoundException e) {
            if (this.permissions == null) {
                // First load, load even if the file doesn't exist
                worldInheritanceCache.clear();
                this.permissions = newPermissions;
                initNewConfiguration();
            }
        } catch (Throwable e) {
            throw new PermissionBackendException("Error loading permissions file!", e);
        }
    }

    /**
     * This method is called when the file the permissions config is supposed to
     * save to does not exist yet,This adds default permissions & stuff
     */
    private void initNewConfiguration() throws PermissionBackendException {
        if (!permissionsFile.exists()) {
            try {
                permissionsFile.createNewFile();

                // Load default permissions
                permissions.set("groups/default/options/default", true);

                List<String> defaultPermissions = new LinkedList<>();
                // Specify here default permissions
                defaultPermissions.add("modifyworld.*");

                permissions.set("groups/default/permissions", defaultPermissions);
                permissions.set("schema-version", getLatestSchemaVersion());

                this.save();
            } catch (IOException e) {
                throw new PermissionBackendException(e);
            }
        }
    }

    @Override
    public void loadFrom(PermissionBackend backend) {
        this.setPersistent(false);
        try {
            super.loadFrom(backend);
        } finally {
            this.setPersistent(true);
        }
        save();
    }

    @Override
    public void setPersistent(boolean persistent) {
        super.setPersistent(persistent);
        this.permissions.setSaveSuppressed(!persistent);
        if (persistent) {
            this.save();
        }
    }

    @Override
    public void writeContents(Writer writer) throws IOException {
        writer.write(this.permissions.saveToString());
    }

    public void save() {
        lock.readLock().lock();
        try {
            this.permissions.save();
        } catch (IOException e) {
            getManager().getLogger().severe("Error while saving permissions file: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }
}
