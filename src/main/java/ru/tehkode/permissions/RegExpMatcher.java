package ru.tehkode.permissions;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpMatcher implements PermissionMatcher {
    protected static Pattern rangeExpression = Pattern.compile("(\\d+)-(\\d+)");

    protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();

    @Override
    public boolean isMatches(String expression, String permission) {
        Pattern permissionMatcher = patternCache.get(expression);

        if (permissionMatcher == null) {
            patternCache.put(expression, permissionMatcher = createPattern(expression));
        }

        return permissionMatcher.matcher(permission).matches();
    }
    
    protected Pattern createPattern(String expression) {
        return Pattern.compile(prepareRegexp(expression), Pattern.CASE_INSENSITIVE);
    }

    public static String prepareRegexp(String expression) {
        if (expression.startsWith("-")) {
            expression = expression.substring(1);
        }

        if (expression.startsWith("#")) {
            expression = expression.substring(1);
        }

        String regexp = expression.replace(".", "\\.").replace("*", "(.*)");

        try {
            Matcher rangeMatcher = rangeExpression.matcher(regexp);
            while (rangeMatcher.find()) {
                StringBuilder range = new StringBuilder();
                int from = Integer.parseInt(rangeMatcher.group(1));
                int to = Integer.parseInt(rangeMatcher.group(2));

                if (from > to) {
                    int temp = from;
                    from = to;
                    to = temp;
                } // swap them

                range.append("(");

                for (int i = from; i <= to; i++) {
                    range.append(i);
                    if (i < to) {
                        range.append("|");
                    }
                }

                range.append(")");

                regexp = regexp.replace(rangeMatcher.group(0), range.toString());
            }
        } catch (Throwable e) {
        }
        
        return regexp;
    }
}
