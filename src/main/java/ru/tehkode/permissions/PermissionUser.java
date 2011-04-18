package ru.tehkode.permissions;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author code
 */
public abstract class PermissionUser extends PermissionNode {

    public PermissionUser(String playerName, PermissionManager manager) {
        super(playerName, manager);
    }

    public Set<PermissionGroup> getGroups() {
        Set<PermissionGroup> groups = new LinkedHashSet<PermissionGroup>();

        for (String group : this.getGroupNames()) {
            groups.add(this.manager.getGroup(group.trim()));
        }

        return groups;
    }

    @Override
    public boolean has(String permission, String world) {
        String expression = this.getMatchingExpression(permission, world);
        if (expression != null) {
            return this.explainExpression(expression);
        }

        for (PermissionGroup group : this.getGroups()){
            if (group.has(permission, world)) {
                return true;
            }
        }

        return false;
    }

    public abstract String[] getGroupNames();
}
