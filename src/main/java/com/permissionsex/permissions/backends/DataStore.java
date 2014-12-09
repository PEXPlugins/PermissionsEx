package com.permissionsex.permissions.backends;

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;

/**
 * Data type abstraction for permissions data
 */
public abstract class DataStore {
	public void reload() {}

	public abstract SubjectData getData(String type, String identifier);
	public abstract boolean isRegistered(String type, String identifier);
	public abstract Iterable<Subject> getAll(String type);
}
