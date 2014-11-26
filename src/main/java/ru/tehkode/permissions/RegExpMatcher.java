package ru.tehkode.permissions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ru.tehkode.permissions.bukkit.ErrorReport;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegExpMatcher implements PermissionMatcher {
	public static final String RAW_REGEX_CHAR = "$";
	protected static Pattern rangeExpression = Pattern.compile("(\\d+)-(\\d+)");

	private final LoadingCache<String, Pattern> patternCache = CacheBuilder.newBuilder().maximumSize(500).build(new CacheLoader<String, Pattern>() {
		@Override
		public Pattern load(String permission) throws Exception {
			return createPattern(permission);
		}
	});

	@Override
	public boolean isMatches(String expression, String permission) {
		try {
			Pattern permissionMatcher = patternCache.get(expression);
			return permissionMatcher.matcher(permission).matches();
		} catch (ExecutionException e) {
			ErrorReport.handleError("While checking for regex match for " + permission + " against expression " + expression, e);
			return false;
		}


	}

	protected static Pattern createPattern(String expression) {
        try {
		    return Pattern.compile(prepareRegexp(expression), Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return Pattern.compile(Pattern.quote(expression), Pattern.CASE_INSENSITIVE);
        }
	}

	public static String prepareRegexp(String expression) {
		if (expression.startsWith("-")) {
			expression = expression.substring(1);
		}

		if (expression.startsWith("#")) {
			expression = expression.substring(1);
		}

		boolean rawRegexp = expression.startsWith(RAW_REGEX_CHAR);
		if (rawRegexp) {
			expression = expression.substring(1);
		}

		String regexp = rawRegexp ? expression : expression.replace(".", "\\.").replace("*", "(.*)");

	/*	try {
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
		} catch (Throwable ignore) {
		}*/

		return regexp;
	}
}
