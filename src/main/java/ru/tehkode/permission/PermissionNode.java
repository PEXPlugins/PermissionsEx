package ru.tehkode.permission;

import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author code
 */
public abstract class PermissionNode {

    protected PermissionManager manager;
    protected String name;

    public PermissionNode(String name, PermissionManager manager) {
        this.manager = manager;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean has(String permission, String world) {
        String expression = getMatchingExpression(permission, world);
        if (expression != null) {
            return this.explainExpression(expression);
        }

        return false;
    }

    protected String getMatchingExpression(String permission, String world) {
        for (String expression : this.getPermissions(world)) {
            String regexp = expression;
            if (regexp.substring(0, 1).equals("-")) {
                regexp = regexp.substring(1);
            }

            // make regular expression
            if (!regexp.substring(0, 1).equals("/")) { // Just convert regular expression
                regexp = regexp.replace(".", "\\.").replace("*", "(.*)");
            } else { // expression are already regular expression
                regexp = regexp.substring(1);
            }

            if (permission.matches(regexp)) {
                return expression;
            }
        }

        return null;
    }

    public abstract void addPermission(String permission, String value, String world);

    public void addPermission(String permission) {
        this.addPermission(permission, "", "");
    }

    public void addPermission(String permission, String value) {
        this.addPermission(permission, value, "");
    }

    public abstract void setPermission(String permission, String value, String world);

    public void setPermission(String permission, String value) {
        this.setPermission(permission, value, "");
    }

    public abstract void removePermission(String permission, String world);

    public void removePermission(String permission) {
        this.removePermission(permission, "");
    }

    public abstract String getPermissionValue(String permission, String world, boolean inheritance);

    public String getPermissionValue(String permission, String world){
        return this.getPermissionValue(permission, world, true);
    }

    public String getPermissionValue(String permission){
        return this.getPermissionValue(permission, "", true);
    }

    protected boolean explainExpression(String expression) {
        return !expression.substring(0, 1).equals("-"); // If expression have - (minus) before then that mean expression are negative
    }

    protected abstract Set<String> getPermissions(String world);

    public abstract String getPrefix();

    public abstract String getPostfix();

    @Override
    public String toString() {
        return this.getName();
    }
}
