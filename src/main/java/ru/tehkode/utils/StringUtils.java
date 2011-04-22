/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package ru.tehkode.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

/**
 *
 * @author code
 */
public class StringUtils {

    public static String implode(String[] array, String separator) {
        if(array.length == 0){
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        for (String str : array) {
            buffer.append(separator);
            buffer.append(str);
        }

        return buffer.substring(separator.length()).trim();
    }

    public static String readStream(InputStream is) throws Exception {
        if (is != null) {
            StringWriter writer = new StringWriter();

            try {
                Reader reader = new InputStreamReader(is, "UTF-8");

                char[] buffer = new char[128];
                int read = 0;
                while((read = reader.read(buffer)) > 0){
                    writer.write(buffer, 0, read);
                }
            } finally {
                is.close();
            }

            return writer.toString();
        }

        return null;
    }
}
