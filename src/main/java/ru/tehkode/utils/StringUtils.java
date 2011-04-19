/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.tehkode.utils;

/**
 *
 * @author code
 */
public class StringUtils {

    public static String implode(String[] array, String separator){
        StringBuilder buffer = new StringBuilder();

        for(String str : array){
            buffer.append(separator);
            buffer.append(str);
        }
        
        return buffer.substring(separator.length()).trim();
    }
}
