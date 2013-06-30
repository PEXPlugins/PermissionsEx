package ru.tehkode.permissions;

import org.junit.Test;

import static org.junit.Assert.*;

public abstract class PermissionMatcherTest {

    protected PermissionMatcher matcher;

    @Test
    public void testCaseInsensitivity() {
        assertTrue(matcher.matches("PERMISSION.*", "permission.Case.Are.does.NOT.matter"));
    }

    @Test
    public void testRanges() {
        // low boundary
        assertTrue(matcher.matches("permission.range.(100-200)", "permission.range.100"));
        // mid range
        assertTrue(matcher.matches("permission.range.(100-200)", "permission.range.150"));
        // high boundary
        assertTrue(matcher.matches("permission.range.(100-200)", "permission.range.200"));

        // out range
        assertFalse(matcher.matches("permission.range.(100-200)", "permission.range.99"));
        assertFalse(matcher.matches("permission.range.(100-200)", "permission.range.201"));

    }

    @Test
    public void testSpecialPermissions() {
        assertTrue(matcher.matches("-permission.*", "permission.anything"));
        assertTrue(matcher.matches("#permission.*", "permission.anything"));

        assertTrue(matcher.matches("-#permission.*", "permission.anything"));

        assertFalse(matcher.matches("permission.*", "#permission.anything"));
    }
}
