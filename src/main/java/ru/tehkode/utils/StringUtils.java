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
