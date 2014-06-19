package ru.tehkode.permissions.data;

/**
 * Represents a qualifier known to PEX
 * TODO Is this even needed?
 */
public class QualifierKey {

	private final Qualifier type;
	private final String value;

	public QualifierKey(Qualifier type, String value) {
		this.type = type;
		this.value = value;
	}

	public Qualifier getType() {
		return this.type;
	}

	public String getValue() {
		return this.value;
	}

	public boolean matches(Context context) {
		return getType().matches(context, getValue());
	}
}
