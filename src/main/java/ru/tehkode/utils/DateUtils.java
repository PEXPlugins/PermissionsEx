package ru.tehkode.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {
    protected final static Pattern INTERVAL_PATTERN = Pattern.compile("((?:\\d+)|(?:\\d+\\.\\d+))\\s*(second|minute|hour|day|week|month|year|s|m|h|d|w)", Pattern.CASE_INSENSITIVE);


    public static int parseInterval(String arg) {
        if (arg.matches("^\\d+$")) {
            return Integer.parseInt(arg);
        }

        Matcher match = INTERVAL_PATTERN.matcher(arg);

        int interval = 0;

        while (match.find()) {
            interval += Math.round(Float.parseFloat(match.group(1)) * getSecondsIn(match.group(2)));
        }

        return interval;
    }

    public static int getSecondsIn(String type) {
        type = type.toLowerCase();

        if ("second".equals(type) || "s".equals(type)) {
            return 1;
        } else if ("minute".equals(type) || "m".equals(type)) {
            return 60;
        } else if ("hour".equals(type) || "h".equals(type)) {
            return 3600;
        } else if ("day".equals(type) || "d".equals(type)) {
            return 86400;
        } else if ("week".equals(type) || "w".equals(type)) {
            return 604800;
        } else if ("month".equals(type)) {
            return 2592000;
        } else if ("year".equals(type)) {
            return 31104000;
        }

        return 0;
    }
}
