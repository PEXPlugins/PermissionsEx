package ru.tehkode.permissions;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionManager {
    protected final static String defaultBackend = "file";

    protected Logger logger = Logger.getLogger("Minecraft");

    protected Map<String, PermissionUser> users = new HashMap<String, PermissionUser>();
    protected Map<String, PermissionGroup> groups = new HashMap<String, PermissionGroup>();
    
    protected PermissionBackend backend = null;
    protected PermissionHandler permissionHandler = new PermissionHandler(this);

    protected PermissionGroup defaultGroup = null;
    protected Configuration config;
    

    public PermissionManager(Configuration config) {
        this.config = config;

        this.initBackend();
    }

    public PermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    public void reset(){
        this.users.clear();
        this.groups.clear();
        this.defaultGroup = null;

        this.backend.reload();
    }

    public PermissionUser getUser(String username) {
        PermissionUser user = users.get(username);

        if (user == null) {
            user = this.backend.getUser(username);
            if (user != null) {
                this.users.put(username, user);
            }
        }

        return user;
    }

    public PermissionGroup getGroup(String groupname) {
        PermissionGroup group = groups.get(groupname);

        if (group == null) {
            group = this.backend.getGroup(groupname);
            if (group != null) {
                this.groups.put(groupname, group);
            }
        }

        return group;
    }

    public PermissionGroup getDefaultGroup(){
        if(this.defaultGroup == null){
            this.defaultGroup = this.backend.getDefaultGroup();
        }

        return this.defaultGroup;
    }

    private void initBackend() {
        String backEnd = this.config.getString("permissions.backend");

        if(backEnd == null){
            backEnd = PermissionManager.defaultBackend; //Default backend
            this.config.setProperty("permissions.backend", backEnd);
            this.config.save();
        }

        try {
            this.setBackend(backEnd);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Selected backend \""+backEnd+"\" are not found. Falling back to file backend");

            try {
                this.setBackend("file");
            } catch (Exception e2) {
                // that mean what author are stupid whore, or smthing break up
                throw new RuntimeException(e2);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error ("+e.getClass().getSimpleName()+") while \""+backEnd+"\" backend initialization. Falling back to file backend");

            try {
                this.setBackend("file");
            } catch (Exception e2) {
                // that mean what author are stupid whore, or smthing break up
                throw new RuntimeException(e2);
            }
        }
    }

    private void setBackend(String backend) throws Exception {
        // Aliases
        if (backend.equals("sql")) {
            backend = "ru.tehkode.permissions.backends.SQLBackend";
        }

        if (backend.equals("file")) {
            backend = "ru.tehkode.permissions.backends.FileBackend";
        }

        Class backendClass = Class.forName(backend);

        this.backend = (PermissionBackend)backendClass.getConstructor(PermissionManager.class, Configuration.class).newInstance(this, this.config);
    }
}
