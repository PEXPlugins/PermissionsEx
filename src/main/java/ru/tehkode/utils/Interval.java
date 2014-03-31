package ru.tehkode.utils;

import java.util.HashMap;
import java.util.Map;

public enum Interval {
    UNKNOWN(0),
    SECOND(1, "second", "s"),
    MINUTE(60, "minute", "m"),
    HOUR(3600, "hour", "h"),
    DAY(86400, "day", "d"),
    WEEK(604800, "week", "w"),
    MONTH(2592000, "month"),
    YEAR(31104000, "year");

    private final int value;

    private final String[] labels;

    private Interval(int seconds, String... labels) {
        // save into private final properties
        this.value = seconds;
        this.labels = labels;
    }

    public int value() {
        return this.value;
    }

    public String[] labels() {
        return this.labels;
    }

    public static Interval byLabel(String label) {
        if(intervalMap.containsKey(label)) {
            return intervalMap.get(label);
        } else {
            return UNKNOWN;
        }
    }

    private final static Map<String, Interval> intervalMap = new HashMap<>();

    static {
        for(Interval type : Interval.values()) {
            for(String label : type.labels()) {
                intervalMap.put(label, type);
            }
        }
    }

}
