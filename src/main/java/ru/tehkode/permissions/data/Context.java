package ru.tehkode.permissions.data;

import ru.tehkode.permissions.EntityType;

import java.util.Date;
import java.util.Set;

/**
 * The context used when determining whether a matcher is valid for a given query
 */
public interface Context {
	public Set<String> getServerTags();

	public String getEntityName();

	public EntityType getEntityType();

	public String getWorld();

	public long getUntil();
}

