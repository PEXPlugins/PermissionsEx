package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsGroupData extends PermissionsData {
    
    
    public List<String> getParents(String worldName);
    
    public void setParents(String worldName, List<String> parents);
}
