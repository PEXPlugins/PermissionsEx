package ru.tehkode.permissions.backends;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.config.ConfigurationNode;
import ru.tehkode.permissions.file.FilePermissionGroup;
import ru.tehkode.permissions.file.FilePermissionUser;

/**
 *
 * @author code
 */
public class FileBackend extends PermissionBackend {

    public Configuration permissions;

    public FileBackend(PermissionManager manager, Configuration config) {
        super(manager, config);

        String permissionFilename = config.getString("permissions.backends.file.file");

        // Default settings
        if (permissionFilename == null) {
            permissionFilename = "permissions.yml";
            config.setProperty("permissions.backends.file.file", "permissions.yml");
            config.save();
        }

        String baseDir = config.getString("permissions.basedir");

        File permissionFile = new File(baseDir, permissionFilename);

        permissions = new Configuration(permissionFile);

        if (!permissionFile.exists()) {
            try {
                permissionFile.createNewFile();

                // Load default permissions
                permissions.setProperty("groups.default.default", true);


                List<String> defaultPermissions = new LinkedList<String>();
                // Specify here default permissions
                defaultPermissions.add("modifyworld.*");

                permissions.setProperty("groups.default.permissions", defaultPermissions);

                permissions.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        permissions.load();
    }

    @Override
    public PermissionUser getUser(String userName) {
        return new FilePermissionUser(userName, manager, this);
    }

    @Override
    public PermissionGroup getGroup(String groupName) {
        return new FilePermissionGroup(groupName, manager, this);
    }

    @Override
    public PermissionGroup getDefaultGroup() {
        PermissionGroup defaultGroup = null;

        

        if (defaultGroup == null) {
            throw new RuntimeException("Default user group are not defined. Please select one with \"default: true\" attribute");
        }

        return defaultGroup;
    }

    @Override
    public PermissionGroup[] getGroups() {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            groups.add(this.manager.getGroup(entry.getKey()));
        }

        return groups.toArray(new PermissionGroup[]{});
    }

    @Override
    public PermissionUser[] getUsers() {
        List<PermissionUser> users = new LinkedList<PermissionUser>();
        Map<String, ConfigurationNode> userMap = this.permissions.getNodesMap("users");

        for (Map.Entry<String, ConfigurationNode> entry : userMap.entrySet()) {
            users.add(this.manager.getUser(entry.getKey()));
        }

        return users.toArray(new PermissionUser[]{});
    }

    @Override
    protected void removeGroupActually(String name) {
        this.permissions.removeProperty("groups."+name);
    }

    @Override
    public void reload() {
        this.permissions.load();
    }
}
