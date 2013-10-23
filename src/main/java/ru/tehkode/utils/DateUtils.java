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

		return Interval.byLabel(type).value();
    }
}
