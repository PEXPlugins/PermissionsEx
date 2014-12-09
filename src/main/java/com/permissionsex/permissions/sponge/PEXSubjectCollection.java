package com.permissionsex.permissions.sponge;

import com.permissionsex.permissions.backends.DataStore;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Collections;

/**
 * Subject collection
 */
public class PEXSubjectCollection implements SubjectCollection {
	private final DataStore data;
	private final SubjectCollection parents;
	private final String type;

	public PEXSubjectCollection(DataStore data, SubjectCollection parents, String type) {
		this.data = data;
		this.parents = parents;
		this.type = type;
	}

	@Override
	public SubjectCollection getParentCollection() {
		return parents;
	}

	@Override
	public Subject get(String identifier) {
		return new PermissionsExSubject(identifier, data.getData(type, identifier));
	}

	@Override
	public boolean hasRegistered(String identifier) {
		return data.isRegistered(type, identifier);
	}

	@Override
	public Iterable<Subject> getAllSubjects() {
		return Collections.emptyList();
	}
}
