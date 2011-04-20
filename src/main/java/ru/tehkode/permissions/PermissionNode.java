package ru.tehkode.permissions;

/**
 *
 * @author code
 */
public abstract class PermissionNode {

    protected PermissionManager manager;
    private String name;
    protected boolean virtual = true;
    protected String prefix = "";
    protected String suffix = "";

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

    public void addPermission(String permission) {
        this.addPermission(permission, "", "");
    }

    public void addPermission(String permission, String value) {
        this.addPermission(permission, value, "");
    }

    public void setPermission(String permission, String value) {
        this.setPermission(permission, value, "");
    }

    public void removePermission(String permission) {
        this.removePermission(permission, "");
    }

    public abstract String getPermissionValue(String permission, String world, boolean inheritance);

    public String getPermissionValue(String permission, String world) {
        return this.getPermissionValue(permission, world, true);
    }

    public String getPermissionValue(String permission) {
        return this.getPermissionValue(permission, "", true);
    }

    protected boolean explainExpression(String expression) {
        return !expression.substring(0, 1).equals("-"); // If expression have - (minus) before then that mean expression are negative
    }

    public void setPermissions(String[] permission) {
        this.setPermissions(permission, null);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String postfix) {
        this.suffix = postfix;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public abstract String[] getPermissions(String world);

    public abstract void addPermission(String permission, String value, String world);

    public abstract void setPermission(String permission, String value, String world);

    public abstract void setPermissions(String[] permissions, String world);

    public abstract void removePermission(String permission, String world);

    public abstract void save();

    public abstract void remove();
}
