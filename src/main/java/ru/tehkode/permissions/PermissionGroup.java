package ru.tehkode.permissions;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author code
 */
public abstract class PermissionGroup extends PermissionNode {

    public PermissionGroup(String groupName, PermissionManager manager) {
        super(groupName, manager);
    }

    @Override
    public boolean has(String permission, String world) {
        String expression = this.getMatchingExpression(permission, world);

        if (expression != null) {
            return this.explainExpression(expression);
        }

        for (PermissionGroup parent : this.getParentGroups()) {
            if (parent.has(permission, world)) {
                return true;
            }
        }

        return false;
    }

    public boolean isChildOf(String groupName, boolean checkInheritance) {
        if (groupName == null || groupName.isEmpty()) {
            return false;
        }

        for (PermissionGroup group : this.getParentGroups()) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return true;
            }

            if (checkInheritance && group.isChildOf(groupName, checkInheritance)) {
                return true;
            }
        }

        return false;
    }

    public PermissionGroup[] getParentGroups() {
        Set<PermissionGroup> parentGroups = new HashSet<PermissionGroup>();

        for (String parentGroup : this.getParentGroupNames()) {

            // Yeah horrible thing, i know, that just safety from invoking empty named groups
            parentGroup = parentGroup.trim();
            if (parentGroup.isEmpty()) {
                continue;
            }

            PermissionGroup group = this.manager.getGroup(parentGroup);
            if (!group.isChildOf(this.getName(), true)) {
                parentGroups.add(group);
            }
        }

        return parentGroups.toArray(new PermissionGroup[]{});
    }

    public boolean isChildOf(String groupName) {
        return this.isChildOf(groupName, false);
    }

    protected abstract String[] getParentGroupNames();

    public abstract void setParentGroups(PermissionGroup[] parentGroups);
}
