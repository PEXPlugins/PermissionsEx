package ru.tehkode.permissions.data;

import ru.tehkode.permissions.EntityType;

import java.util.Set;

/**
 * @author zml2008
 */
public class MutableContext implements Context {
	@Override
	public Set<String> getServerTags() {
		return null;
	}

	@Override
	public String getEntityName() {
		return null;
	}

	@Override
	public EntityType getEntityType() {
		return null;
	}

	@Override
	public String getWorld() {
		return null;
	}

	@Override
	public long getUntil() {
		return 0;
	}
}
