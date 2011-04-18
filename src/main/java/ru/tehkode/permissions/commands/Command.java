/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.tehkode.permissions.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author code
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();
    String syntax();
    String description();
    String permission();
}
