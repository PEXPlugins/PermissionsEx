package com.permissionsex.permissions.sponge;

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;

import java.util.Collections;
import java.util.List;

/**
 * Permissions subject implementation
 */
public class PermissionsExSubject implements Subject {
	private final SubjectData data, transientData;
	private final String identifier;

	public PermissionsExSubject(String identifier, SubjectData data) {
		this.data = data;
		this.identifier = identifier;
		this.transientData = null; // TODO add MemoryData
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public SubjectData getData() {
		return data;
	}

	@Override
	public SubjectData getTransientData() {
		return transientData;
	}

	@Override
	public boolean hasPermission(List<Context> contexts, String permission) {
		return false;
	}

	@Override
	public boolean hasPermission(String permission) {
		return false;
	}

	@Override
	public boolean childOf(String parent) {
		return false;
	}

	public boolean childOf(Subject parent) {
		return false;
	}

	@Override
	public List<Context> getActiveContexts() {
		return Collections.emptyList();
	}

	@Override
	public List<Subject> getParents() {
		return Collections.emptyList();
	}


}
