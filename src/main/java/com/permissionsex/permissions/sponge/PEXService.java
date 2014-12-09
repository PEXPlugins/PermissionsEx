package com.permissionsex.permissions.sponge;

import com.google.common.base.Optional;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.context.ContextCalculator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles core management stuff
 */
public class PEXService implements PermissionService {
	private final List<ContextCalculator> contextCalculators = new CopyOnWriteArrayList<>();
	@Override
	public SubjectCollection getUserSubjects() {
		return null;
	}

	@Override
	public SubjectCollection getGroupSubjects() {
		return null;
	}

	@Override
	public void registerContextCalculator(ContextCalculator calculator) {
		contextCalculators.add(calculator);
	}

	@Override
	public Optional<SubjectCollection> getSubjects(String identifier, SubjectCollection parentCollection) {
		return null;
	}

	public void close() {

	}
}
