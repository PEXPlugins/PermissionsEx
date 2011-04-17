package ru.tehkode.permission.file;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import ru.tehkode.permission.PermissionGroup;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.backends.FileBackend;
import ru.tehkode.permission.config.Configuration;
import ru.tehkode.permission.config.ConfigurationNode;

/**
 *
 * @author code
 */
public class FilePermissionGroup extends PermissionGroup {

    protected ConfigurationNode node;
    protected FileBackend backend;
    protected boolean virtual = false;

    public FilePermissionGroup(String name, PermissionManager manager, FileBackend backend) {
        super(name, manager);

        this.backend = backend;

        this.node = backend.permissions.getNode("groups." + name);
        if (this.node == null) {
            this.node = Configuration.getEmptyNode();
            this.virtual = true;
        }
    }

    @Override
    public Set<PermissionGroup> getParentGroups() {
        Set<PermissionGroup> parentGroups = new HashSet<PermissionGroup>();

        List<String> parents = this.node.getStringList("inheritance", null);

        if (parents != null) {
            for (String parentGroup : parents) {
                parentGroups.add(this.manager.getGroup(parentGroup));
            }
        }

        return parentGroups;
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
            for (PermissionGroup group : this.getParentGroups()) {
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
        String nodePath = value != null && !value.isEmpty() ? "options" : "permissions";
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
            this.backend.permissions.setProperty("groups." + this.getName(), this.node);
        }

        this.backend.permissions.save();
    }
}
