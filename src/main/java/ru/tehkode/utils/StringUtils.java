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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

public class StringUtils {

    public static String implode(String[] array, String separator) {
        if (array.length == 0) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();

        for (String str : array) {
            buffer.append(separator);
            buffer.append(str);
        }

        return buffer.substring(separator.length()).trim();
    }

    public static String implode(List<?> list, String separator) {
        if (list.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        int lastElement = list.size() - 1;
        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(0).toString());

            if (i < lastElement) {
                builder.append(separator);
            }
        }
        for (Object obj : list) {
            builder.append(obj.toString()).append(separator);
        }

        return builder.toString();
    }

    public static String readStream(InputStream is) throws Exception {
        if (is != null) {
            StringWriter writer = new StringWriter();

            try {
                Reader reader = new InputStreamReader(is, "UTF-8");

                char[] buffer = new char[128];
                int read = 0;
                while ((read = reader.read(buffer)) > 0) {
                    writer.write(buffer, 0, read);
                }
            } finally {
                is.close();
            }

            return writer.toString();
        }

        return null;
    }

    public static String repeat(String str, int times) {
        final StringBuilder buffer = new StringBuilder(times * str.length());
        for (int i = 0; i < times; i++) {
            buffer.append(str);
        }
        return buffer.toString();

    }

    public static int toInteger(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
