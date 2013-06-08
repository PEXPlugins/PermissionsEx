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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class StringUtils {

	/**
	 * Returns the given array joined by a separator.
	 *
	 * @param array     an array of strings to join
	 * @param separator a string to insert between the array elements
	 * @return the full string result
	 */
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

	/**
	 * Returns the elements joined by a separator.
	 *
	 * @param list      a List object to join together
	 * @param separator a string to insert between the list elements
	 * @return the full string result
	 */
	public static String implode(List<?> list, String separator) {
		if (list.isEmpty()) {
			return "";
		}

		StringBuilder buffer = new StringBuilder();

		int lastElement = list.size() - 1;
		for (int i = 0; i < list.size(); i++) {
			buffer.append(list.get(i).toString());

			if (i < lastElement) {
				buffer.append(separator);
			}
		}

		return buffer.toString();
	}

	/**
	 * Return a stream's complete input as a string, or null if the InputStream is null
	 *
	 * @param is an InputStream to read from
	 * @return a String representing the input read from the InputStream
	 */
	public static String readStream(InputStream is) throws IOException {
		if (is != null) {
			StringBuilder builder = new StringBuilder();

			try {
				Reader reader = new InputStreamReader(is, "UTF-8");

				char[] buffer = new char[128];
				int read;
				while ((read = reader.read(buffer)) > 0) {
					builder.append(buffer, 0, read);
				}
			} finally {
				is.close();
			}

			return builder.toString();
		}

		return null;
	}

	/**
	 * Repeat a string a given number of times.
	 *
	 * @param str   the string to repeat
	 * @param times the number of times to repeat the string
	 * @return the completed repeating
	 */
	public static String repeat(String str, int times) {
		StringBuilder buffer = new StringBuilder(times * str.length());
		for (int i = 0; i < times; i++) {
			buffer.append(str);
		}
		return buffer.toString();

	}

	/**
	 * Parse a string to an integer value, using a given default on fail
	 *
	 * @param value        a String to parse to an int
	 * @param defaultValue the default, used when 'value' is not an integer
	 * @return the parsed value
	 */
	public static int toInteger(String value, int defaultValue) {
		int ret = defaultValue;
		try {
			ret = Integer.parseInt(value);
		} catch (NumberFormatException e) {
		}
		return ret;
	}
}
