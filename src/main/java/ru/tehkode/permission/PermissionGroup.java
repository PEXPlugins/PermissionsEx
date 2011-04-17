package ru.tehkode.permission;

import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author code
 */
public abstract class PermissionGroup extends PermissionNode {
    public PermissionGroup(String groupName, PermissionManager manager){
        super(groupName, manager);
    }

    @Override
    public boolean has(String permission, String world){
        String expression = this.getMatchingExpression(permission, world);
        if(expression != null){
            return this.explainExpression(expression);
        }

        for (PermissionGroup parent : this.getParentGroups()){
            if(parent.has(permission, world)){
                return true;
            }
        }

        return false;
    }

    public abstract Set<PermissionGroup> getParentGroups();

}
