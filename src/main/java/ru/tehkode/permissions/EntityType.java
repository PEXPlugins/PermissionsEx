package ru.tehkode.permissions;

import ru.tehkode.permissions.data.Qualifier;

/**
* @author zml2008
*/
public enum EntityType {
	USER(Qualifier.USER),
	GROUP(Qualifier.GROUP);

	private final Qualifier associatedQual;

	EntityType(Qualifier associatedQual) {
		this.associatedQual = associatedQual;
	}

	public Qualifier getQualifier() {
		return associatedQual;
	}
}
