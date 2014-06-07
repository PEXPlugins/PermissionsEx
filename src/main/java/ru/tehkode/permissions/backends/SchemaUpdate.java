package ru.tehkode.permissions.backends;


import ru.tehkode.permissions.exceptions.PermissionBackendException;

/**
 * Class designed to perform updates to schemas.
 */
public abstract class SchemaUpdate implements Comparable<SchemaUpdate> {
	private final int nextVersion;

	public SchemaUpdate(int nextVersion) {
		this.nextVersion = nextVersion;
	}

	public int getUpdateVersion() {
		return this.nextVersion;
	}

	public abstract void performUpdate() throws PermissionBackendException;

	@Override
	public int compareTo(SchemaUpdate schemaUpdate) {
		return Integer.valueOf(this.nextVersion).compareTo(schemaUpdate.nextVersion);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{version=" + getUpdateVersion() + "}";
	}
}
