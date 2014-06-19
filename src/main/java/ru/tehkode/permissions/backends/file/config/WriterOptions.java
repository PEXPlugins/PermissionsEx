package ru.tehkode.permissions.backends.file.config;

import java.util.regex.Pattern;

/**
* @author zml2008
*/
public class WriterOptions {
	public static final Pattern SPECIAL_CHARACTERS = Pattern.compile("([\\]\n\r\\\\#])");
	public enum ListFormat {
		DASH("-"),
		STAR("*"),
		PLUS("+");

		private final String start;

		ListFormat(String start) {
			this.start = start;
		}

		public String getStartChar() {
			return this.start;
		}
	}

	public enum MappingFormat {
		EQUALS("="),
		ARROW("->"),
		COLON(": ");


		private final String divider;

		MappingFormat(String divider) {
			this.divider = divider;
		}

		public String getDivider() {
			return divider;
		}
	}

	public enum StringType {
		PLAIN,
		SINGLE_QUOTED,
		DOUBLE_QUOTED,
		HEREDOC
	}


	public StringType getStringType(String toWrite) {
		boolean containsSingle = toWrite.contains("\'");
		boolean containsDouble = toWrite.contains("\"");
		boolean containsForbidden = toWrite.contains(getMappingFormat().getDivider());

		if (!containsForbidden) {
			containsForbidden = SPECIAL_CHARACTERS.matcher(toWrite).find();
		}

		if (containsForbidden) {
			if (containsDouble && containsSingle) {
				return StringType.HEREDOC;
			} else {
				return containsDouble ? StringType.SINGLE_QUOTED : StringType.DOUBLE_QUOTED;
			}
		} else if (containsSingle && !containsDouble) {
			return StringType.DOUBLE_QUOTED;
		} else if (containsDouble) {
			return containsSingle ? StringType.HEREDOC : StringType.SINGLE_QUOTED;
		} else {
			return StringType.PLAIN;
		}
	}

	public ListFormat getListFormat() {
		return ListFormat.DASH;
	}

	public MappingFormat getMappingFormat() {
		return MappingFormat.EQUALS;
	}
}
