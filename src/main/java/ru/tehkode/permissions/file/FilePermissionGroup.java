package ru.tehkode.permissions.file;

import com.avaje.ebeaninternal.server.expression.LikeExpressionLucene;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.config.ConfigurationNode;

/**
 *
 * @author code
 */
public class FilePermissionGroup extends PermissionGroup {

    protected ConfigurationNode node;
    protected FileBackend backend;
    
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
    public String[] getParentGroupsNamesImpl(){
        return this.node.getStringList("inheritance", new LinkedList<String>()).toArray(new String[0]);
    }

    @Override
    public String getPrefix() {
        return this.node.getString("prefix", "");
    }

    @Override
    public String getSuffix() {
        return this.node.getString("postfix", "");
    }

    @Override
    public void setSuffix(String postfix) {
        super.setSuffix(postfix);
    }

    @Override
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
    }

    @Override
    public String[] getPermissions(String world) {
        Set<String> permissions = new LinkedHashSet<String>();

        List<String> worldPermissions = this.node.getStringList("worlds." + world + ".permissions", null); // world specific permissions
        if (worldPermissions != null) {
            permissions.addAll(worldPermissions);
        }

        List<String> commonPermissions = this.node.getStringList("permissions", null);
        if (commonPermissions != null) {
            permissions.addAll(commonPermissions);
        }

        return permissions.toArray(new String[]{});
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

    @Override
    public void setPermissions(String[] permissions, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }

        this.node.setProperty(nodePath, Arrays.asList(permissions));
    }

    @Override
    public void setParentGroups(PermissionGroup[] parentGroups) {
        if(parentGroups == null){
            return;
        }

        List<PermissionGroup> newParents = Arrays.asList(parentGroups);

        List<String> parents = this.node.getStringList("inheritance", new LinkedList<String>());

        parents.clear();
        for (PermissionGroup parent : newParents) {
            parents.add(parent.getName());
        }

        this.node.setProperty("inheritance", parents);

    }

    public void save() {
        if (this.virtual) {
            this.backend.permissions.setProperty("groups." + this.getName(), this.node);
        }

        this.backend.permissions.save();
    }

    @Override
    public void remove(){
        if(!this.virtual) {
            this.backend.permissions.setProperty("groups." + this.getName(), null);
        }

        this.backend.permissions.save();
    }
}
