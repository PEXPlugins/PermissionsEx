/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions;

import java.util.logging.Logger;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionBackendFactory {

    protected final static String defaultBackend = "file";

    /**
     * @todo Make this thing reconfigurable and flexible. Think about it
     *
     * @param alias
     * @return String - classname for given alias
     */
    public static String getBackendClassName(String alias) {

        if (alias.equalsIgnoreCase("sql")) {
            return "ru.tehkode.permissions.backends.SQLBackend";
        } else if (alias.equalsIgnoreCase("file")) {
            return "ru.tehkode.permissions.backends.FileBackend";
        }

        return alias;
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }

        String className = getBackendClassName(backendName);

        try {
            return (PermissionBackend)Class.forName(className).getConstructor(PermissionManager.class, Configuration.class).newInstance(manager, config);
        } catch (ClassNotFoundException e) {
            Logger.getLogger("Minecraft").severe("Selected backend \""+backendName+"\" are not found. Falling back to file backend");
            
            if(!className.equals(getBackendClassName(defaultBackend))){
                return getBackend(defaultBackend, manager, config);
            } else {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
