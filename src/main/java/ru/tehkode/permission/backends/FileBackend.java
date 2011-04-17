/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permission.backends;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import ru.tehkode.permission.PermissionBackend;
import ru.tehkode.permission.PermissionGroup;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.PermissionUser;
import ru.tehkode.permission.config.Configuration;
import ru.tehkode.permission.config.ConfigurationNode;
import ru.tehkode.permission.file.FilePermissionGroup;
import ru.tehkode.permission.file.FilePermissionUser;

/**
 *
 * @author code
 */
public class FileBackend extends PermissionBackend {

    public Configuration permissions;

    public FileBackend(PermissionManager manager, Configuration config) {
        super(manager, config);

        String permissionFilename = config.getString("permissions.backends.file.file");

        // Default setting
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
                permissions.setProperty("groups.default.permissions.build", true);

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

        Map<String, ConfigurationNode> groupsMap = this.permissions.getN("groups");

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            if (entry.getValue().getBoolean("default", false)) {
                defaultGroup = this.manager.getGroup(entry.getKey()); // we found what we looking for, bailout :)
                break;
            }

        }

        if (defaultGroup == null) {
            throw new RuntimeException("Default user group are not defined. Please select one with \"default: true\" attribute");
        }

        return defaultGroup;
    }
}
