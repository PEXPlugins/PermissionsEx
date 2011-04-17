package ru.tehkode.permission.file;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import ru.tehkode.permission.PermissionGroup;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.PermissionUser;
import ru.tehkode.permission.backends.FileBackend;
import ru.tehkode.permission.config.Configuration;
import ru.tehkode.permission.config.ConfigurationNode;

/**
 *
 * @author code
 */
public class FilePermissionUser extends PermissionUser {

    protected ConfigurationNode node;
    protected FileBackend backend;
    protected Boolean virtual = false;

    public FilePermissionUser(String playerName, PermissionManager manager, FileBackend backend) {
        super(playerName, manager);

        this.backend = backend;
        this.node = backend.permissions.getNode("users." + playerName);
        if (this.node == null) {
            this.node = Configuration.getEmptyNode();
            this.virtual = true;
        }
    }

    @Override
    public String getPrefix() {
        return this.node.getString("prefix", "");
    }

    @Override
    public String getPostfix() {
        return this.node.getString("postfix", "");
    }

    @Override
    protected Set<String> getPermissions(String world) {
        Set<String> permissions = new LinkedHashSet<String>();

        List<String> worldPermissions = this.node.getStringList("worlds." + world + ".permissions", null); // world specific permissions
        if (worldPermissions != null) {
            permissions.addAll(worldPermissions);
        }

        List<String> commonPermissions = this.node.getStringList("permissions", null);
        if (commonPermissions != null) {
            permissions.addAll(commonPermissions);
        }

        return permissions;
    }

    @Override
    public String[] getGroupNames() {
        String groups = this.node.getString("group");
        if (groups == null) {
            return new String[]{this.manager.getDefaultGroup().getName()};
        } else if (groups.contains(",")) {
            return groups.split(",");
        } else {
            return new String[]{groups};
        }
    }

    @Override
    public String getPermissionValue(String permission, String world, boolean inheritance) {
        if (world != null && !world.isEmpty()) {
            String worldPermission = this.node.getString("worlds." + world + ".options." + permission);
            if (worldPermission != null && !worldPermission.isEmpty()) {
                return worldPermission;
            }
        }

        String commonPermission = this.node.getString("options." + permission);
        if (commonPermission != null && !commonPermission.isEmpty()) {
            return commonPermission;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getGroups()) {
                String value = group.getPermissionValue(permission, world, inheritance);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return "";
    }

    @Override
    public void addPermission(String permission, String value, String world) {
        String nodePath = (value != null && !value.isEmpty()) ? "options" : "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath += ".worlds." + world + "." + nodePath;
        }

        if (value != null && !value.isEmpty()) {
            nodePath += "." + permission;
            this.node.setProperty(nodePath, value);
        } else {
            List<String> permissions = this.node.getStringList(nodePath, new LinkedList<String>());
            if (!permissions.contains(permission)) {
                permissions.add(permission);
            }
            this.node.setProperty(nodePath, permissions);
        }

        this.save();
    }

    @Override
    public void setPermission(String permission, String value, String world) {
        String nodePath = "options";
        if (world != null && !world.isEmpty()) {
            nodePath += ".worlds." + world + "." + nodePath;
        }

        if (value != null && !value.isEmpty()) {
            nodePath += "." + permission;
            this.node.setProperty(nodePath, value);
        } else {
            this.node.removeProperty(nodePath);
        }

        this.save();
    }

    @Override
    public void removePermission(String permission, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath += "worlds." + world + "." + nodePath;
        }

        List<String> permissions = this.node.getStringList(nodePath, new LinkedList<String>());
        if (permissions.contains(permission)) {
            permissions.remove(permission);
            this.node.setProperty(nodePath, permissions);
        }

        this.save();
    }

    public void save() {
        if (this.virtual) {
            if(this.node.getString("group", null) == null){ // Set default group
                this.node.setProperty("group", this.manager.getDefaultGroup().getName());
            }
            this.backend.permissions.setProperty("users." + this.getName(), node);
        }

        this.backend.permissions.save();
    }
}
