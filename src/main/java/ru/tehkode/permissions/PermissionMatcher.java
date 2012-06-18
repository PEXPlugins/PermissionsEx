package ru.tehkode.permissions;

public interface PermissionMatcher {
    
    public boolean isMatches(String expression, String permission);
    
}
