package ru.tehkode.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {
	protected final static Pattern INTERVAL_PATTERN = Pattern.compile("((?:\\d+)|(?:\\d+\\.\\d+))\\s*(second|minute|hour|day|week|month|year|s|m|h|d|w)", Pattern.CASE_INSENSITIVE);

	static Map<String,Integer> secondsMap;
	{
	    Map<String,Integer> temp = new HashMap<String, Integer>();
	    temp.put("second",1);	temp.put("s",1);
	    temp.put("minute",60);	temp.put("m",60);
	    temp.put("hour",3600);	temp.put("h",3600);
	    temp.put("day",86400);	temp.put("d",86400);
	    temp.put("week",604800);	temp.put("w",604800);
	    temp.put("month",2592000);
	    temp.put("year",31104000);
	    secondsMap = Collections.unmodifiableMap(temp);
	}

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

		if (secondsMap.containsKey(type)) {
			return secondsMap.get(type);
		} else {
			return 0;
		}
	}
}
